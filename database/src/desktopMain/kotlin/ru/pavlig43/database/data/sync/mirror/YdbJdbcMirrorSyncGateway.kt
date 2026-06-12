package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.data.sync.defaultUpdatedAt
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

/**
 * JDBC-реализация typed mirror gateway для YDB.
 *
 * Gateway лениво проверяет и создает таблицы по DDL соответствующего
 * [YdbMirrorRowCodec], загружает полные snapshots и выполняет batch UPSERT.
 * Успешно проверенные таблицы кэшируются на время жизни объекта.
 *
 * Важно: текущий UPSERT не содержит server-side compare-and-set по версии.
 * Выбор winners выполняется до записи, поэтому два конкурентных клиента теоретически
 * могут записать устаревший snapshot. Этот риск должен устраняться на уровне YDB.
 */
class YdbJdbcMirrorSyncGateway(
    private val config: YdbMirrorJdbcConfig,
) : MirrorSyncRemoteGateway {
    private val ensuredTables = ConcurrentHashMap.newKeySet<MirrorSyncTable>()
    private val ensureTableLock = Any()

    /**
     * Проверяет соединение и гарантирует наличие всех поддерживаемых typed tables.
     *
     * Ожидаемые JDBC/schema-ошибки возвращаются в [MirrorRemoteStatus.error], чтобы
     * UI мог показать диагностику без исключения.
     */
    override suspend fun getStatus(): MirrorRemoteStatus {
        return runCatching {
            withConnection { connection ->
                supportedYdbMirrorCodecs.values.forEach { codec ->
                    ensureTable(connection, codec)
                }
            }
            MirrorRemoteStatus(
                configured = true,
                availableTables = supportedYdbMirrorCodecs.keys.mapTo(mutableSetOf()) { it.tableName },
                checkedAt = defaultUpdatedAt(),
            )
        }.getOrElse { throwable ->
            MirrorRemoteStatus(
                configured = true,
                availableTables = emptySet(),
                checkedAt = defaultUpdatedAt(),
                error = throwable.message ?: "Mirror YDB connection failed",
            )
        }
    }

    /** Загружает полный snapshot запрошенных таблиц через их typed codecs. */
    override suspend fun loadRemoteSnapshot(
        tables: List<MirrorSyncTable>,
    ): Result<MirrorRemoteSnapshot> {
        return runCatching {
            val codecs = requireSupportedCodecs(tables)
            val rows = withConnection { connection ->
                codecs.associate { codec ->
                    ensureTable(connection, codec)
                    codec.table to loadRows(connection, codec)
                }
            }
            MirrorRemoteSnapshot(
                loadedAt = defaultUpdatedAt(),
                rowsByTable = rows,
            )
        }
    }

    /**
     * Группирует changes по таблицам и выполняет typed batch UPSERT.
     *
     * Пустой список допустим и возвращает результат без затронутых таблиц.
     */
    override suspend fun pushMirrorState(
        changes: List<MirrorPushEntityChange>,
    ): Result<MirrorPushResult> {
        return runCatching {
            val tables = changes.map(MirrorPushEntityChange::table).distinct()
            val codecs = requireSupportedCodecs(tables).associateBy(YdbMirrorRowCodec::table)
            withConnection { connection ->
                changes.groupBy(MirrorPushEntityChange::table).forEach { (table, tableChanges) ->
                    val codec = codecs.getValue(table)
                    ensureTable(connection, codec)
                    upsertRows(connection, codec, tableChanges.map(MirrorPushEntityChange::row))
                }
            }
            MirrorPushResult(
                pushedAt = defaultUpdatedAt(),
                affectedTables = tables.toSet(),
            )
        }
    }

    /**
     * Представляет удаленный snapshot как плоский список изменений.
     *
     * Метод не применяет данные к Room и не выполняет reconciliation.
     */
    override suspend fun pullMirrorState(
        request: MirrorPullRequest,
    ): Result<MirrorPullResult> {
        return loadRemoteSnapshot(request.tables).map { snapshot ->
            MirrorPullResult(
                pulledAt = snapshot.loadedAt,
                changes = request.tables.flatMap { table ->
                    snapshot.rowsByTable[table].orEmpty().map { row ->
                        MirrorPushEntityChange(table = table, row = row)
                    }
                },
            )
        }
    }

    /**
     * Физически удаляет строки remote mirror; предназначен для smoke cleanup.
     *
     * Основной sync-протокол использует tombstone и не должен вызывать этот метод.
     */
    internal suspend fun deleteRows(
        syncIdsByTable: Map<MirrorSyncTable, List<String>>,
    ): Result<Int> {
        return runCatching {
            var deletedRows = 0
            withConnection { connection ->
                syncIdsByTable.forEach { (table, syncIds) ->
                    if (syncIds.isEmpty()) return@forEach
                    val codec = supportedYdbMirrorCodecs.getValue(table)
                    ensureTable(connection, codec)
                    connection.prepareStatement(
                        """
                        DELETE FROM `${config.tablePath(table)}`
                        WHERE sync_id = CAST(? AS Utf8)
                        """.trimIndent()
                    ).use { statement ->
                        syncIds.forEach { syncId ->
                            statement.setString(1, syncId)
                            statement.addBatch()
                        }
                        statement.executeBatch()
                    }
                    deletedRows += syncIds.size
                }
            }
            deletedRows
        }
    }

    private fun requireSupportedCodecs(
        tables: List<MirrorSyncTable>,
    ): List<YdbMirrorRowCodec> {
        val unsupported = tables.filterNot(supportedYdbMirrorCodecs::containsKey)
        require(unsupported.isEmpty()) {
            "Mirror YDB codecs are not implemented for: ${unsupported.joinToString { it.tableName }}"
        }
        return tables.map(supportedYdbMirrorCodecs::getValue)
    }

    private fun ensureTable(
        connection: Connection,
        codec: YdbMirrorRowCodec,
    ) {
        if (codec.table in ensuredTables) return

        synchronized(ensureTableLock) {
            if (codec.table in ensuredTables) return

            val tablePath = config.tablePath(codec.table)
            if (!tableExists(connection, tablePath)) {
                // YDB ограничивает параллельные schema operations, поэтому создание
                // таблиц сериализовано и дополнено retry ниже.
                createTableWithRetry(connection, codec.createTableSql(tablePath))
            }
            ensuredTables += codec.table
        }
    }

    private fun tableExists(
        connection: Connection,
        tablePath: String,
    ): Boolean {
        return try {
            connection.createStatement().use { statement ->
                statement.executeQuery("SELECT sync_id FROM `$tablePath` LIMIT 0").use { }
            }
            true
        } catch (_: SQLException) {
            false
        }
    }

    private fun createTableWithRetry(
        connection: Connection,
        createTableSql: String,
    ) {
        var retryDelayMillis = INITIAL_SCHEMA_RETRY_DELAY_MILLIS
        repeat(MAX_SCHEMA_CREATE_ATTEMPTS) { attempt ->
            try {
                connection.createStatement().use { statement ->
                    statement.execute(createTableSql)
                }
                return
            } catch (throwable: SQLException) {
                val canRetry = throwable.isSchemaOperationLimitError() &&
                    attempt < MAX_SCHEMA_CREATE_ATTEMPTS - 1
                if (!canRetry) throw throwable
                Thread.sleep(retryDelayMillis)
                retryDelayMillis = (retryDelayMillis * 2).coerceAtMost(MAX_SCHEMA_RETRY_DELAY_MILLIS)
            }
        }
    }

    private fun loadRows(
        connection: Connection,
        codec: YdbMirrorRowCodec,
    ): List<MirrorSyncRow> {
        connection.createStatement().use { statement ->
            statement.executeQuery(codec.selectAllSql(config.tablePath(codec.table))).use { resultSet ->
                return buildList {
                    while (resultSet.next()) {
                        add(codec.read(resultSet))
                    }
                }
            }
        }
    }

    private fun upsertRows(
        connection: Connection,
        codec: YdbMirrorRowCodec,
        rows: List<MirrorSyncRow>,
    ) {
        if (rows.isEmpty()) return
        connection.prepareStatement(codec.upsertSql(config.tablePath(codec.table))).use { statement ->
            rows.forEach { row ->
                codec.bind(statement, row)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun <T> withConnection(
        block: (Connection) -> T,
    ): T {
        val properties = Properties().apply {
            val serviceAccountFile = config.serviceAccountFile?.takeIf(String::isNotBlank)
            if (serviceAccountFile != null) {
                setProperty("saKeyFile", serviceAccountFile)
            } else {
                config.authToken
                    ?.takeIf(String::isNotBlank)
                    ?.let { setProperty("token", it) }
            }
        }
        return DriverManager.getConnection(config.jdbcUrl, properties).use(block)
    }

    private companion object {
        const val MAX_SCHEMA_CREATE_ATTEMPTS = 6
        const val INITIAL_SCHEMA_RETRY_DELAY_MILLIS = 500L
        const val MAX_SCHEMA_RETRY_DELAY_MILLIS = 4_000L
    }
}

/** Определяет временную YDB-ошибку лимита schema operations, пригодную для retry. */
internal fun SQLException.isSchemaOperationLimitError(): Boolean {
    return generateSequence<Throwable>(this) { it.cause }
        .mapNotNull(Throwable::message)
        .any { message ->
            message.contains("limit on the number of schema operations", ignoreCase = true) ||
                message.contains("try again later", ignoreCase = true)
        }
}
