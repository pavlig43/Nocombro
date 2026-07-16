package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.datetime.getCurrentLocalDateTime
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties
import kotlin.time.TimeSource

/**
 * JDBC-реализация typed mirror gateway для YDB.
 *
 * Gateway работает с уже созданными typed tables, загружает полные snapshots и
 * выполняет условную запись по версии. Схему YDB нужно создавать и менять отдельным SQL.
 */
@Suppress("TooManyFunctions")
class YdbJdbcMirrorSyncGateway(
    private val config: YdbMirrorJdbcConfig,
) : MirrorSyncRemoteGateway {
    override suspend fun getConfigurationStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = emptySet(),
        checkedAt = getCurrentLocalDateTime(),
    )

    /**
     * Проверяет соединение и доступность всех поддерживаемых typed tables.
     *
     * Ожидаемые JDBC-ошибки возвращаются в [MirrorRemoteStatus.error], чтобы UI
     * мог показать диагностику без исключения.
     */
    override suspend fun getStatus(): MirrorRemoteStatus {
        return runCatching {
            val availableTables = withConnection { connection ->
                supportedYdbMirrorCodecs.values.mapTo(mutableSetOf()) { codec ->
                    checkTableReadable(connection, codec)
                    codec.table.tableName
                }
            }
            MirrorRemoteStatus(
                configured = true,
                availableTables = availableTables,
                checkedAt = getCurrentLocalDateTime(),
            )
        }.getOrElse { throwable ->
            MirrorRemoteStatus(
                configured = true,
                availableTables = emptySet(),
                checkedAt = getCurrentLocalDateTime(),
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
                    codec.table to loadRows(connection, codec)
                }
            }
            MirrorRemoteSnapshot(
                loadedAt = getCurrentLocalDateTime(),
                rowsByTable = rows,
            )
        }
    }

    /**
     * Условно записывает каждую typed строку и читает фактического победителя.
     *
     * Равная или более новая версия YDB не перезаписывается. Результат делит
     * входной список на принятые и отклонённые строки; ошибка содержит таблицу,
     * `sync_id` и исходный текст YDB. Пустой список допустим.
     */
    override suspend fun pushMirrorState(
        changes: List<MirrorPushEntityChange>,
    ): Result<MirrorPushResult> {
        return runCatching {
            val tables = changes.map(MirrorPushEntityChange::table).distinct()
            val codecs = requireSupportedCodecs(tables).associateBy(YdbMirrorRowCodec::table)
            val accepted = mutableListOf<MirrorPushEntityChange>()
            val rejected = mutableListOf<MirrorPushRejection>()
            withConnection { connection ->
                changes.forEach { change ->
                    val codec = codecs.getValue(change.table)
                    val remoteRow = runCatching {
                        conditionalUpsertRow(connection, codec, change.row)
                    }.getOrElse { throwable ->
                        throw IllegalStateException(
                            "Mirror push failed: table=${change.table.tableName}, " +
                                "sync_id=${change.row.syncId}: ${throwable.message ?: throwable}",
                            throwable,
                        )
                    }
                    if (remoteRow.hasSameSyncContent(change.row)) {
                        accepted += change
                    } else {
                        rejected += MirrorPushRejection(
                            change = change,
                            remoteRow = remoteRow,
                            reason = if (remoteRow.versionAt() == change.row.versionAt()) {
                                MirrorPushRejectionReason.EQUAL_VERSION_CONFLICT
                            } else {
                                MirrorPushRejectionReason.STALE_VERSION
                            },
                        )
                    }
                }
            }
            MirrorPushResult(
                pushedAt = getCurrentLocalDateTime(),
                affectedTables = accepted.mapTo(mutableSetOf(), MirrorPushEntityChange::table),
                acceptedChanges = accepted,
                rejectedChanges = rejected,
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

    private fun checkTableReadable(
        connection: Connection,
        codec: YdbMirrorRowCodec,
    ) {
        connection.createStatement().use { statement ->
            statement.executeQuery(codec.selectProbeSql(config.tablePath(codec.table))).use { }
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
        val connectionMark = TimeSource.Monotonic.markNow()
        val connection = DriverManager.getConnection(config.jdbcUrl, properties)
        LOGGER.fine("Mirror sync stage=connection durationMs=${connectionMark.elapsedNow().inWholeMilliseconds}")
        return connection.use(block)
    }

    /**
     * Условно записывает строку и читает победителя из того же serializable DML.
     *
     * @return входная строка после принятия либо более новая строка YDB.
     * @throws IllegalStateException если запрос не вернул итоговую строку.
     */
    private fun conditionalUpsertRow(
        connection: Connection,
        codec: YdbMirrorRowCodec,
        row: MirrorSyncRow,
    ): MirrorSyncRow {
        connection.prepareStatement(codec.conditionalUpsertSql(config.tablePath(codec.table))).use { statement ->
            codec.bind(statement, row)
            var parameterIndex = codec.columnNames.size + 1
            statement.setString(parameterIndex++, row.syncId)
            statement.setString(parameterIndex++, row.versionAt().toString())
            statement.setString(parameterIndex, row.syncId)

            var hasResultSet = statement.execute()
            while (!hasResultSet && statement.updateCount != -1) {
                hasResultSet = statement.moreResults
            }
            check(hasResultSet) {
                "Conditional mirror upsert returned no row: " +
                    "table=${codec.table.tableName}, sync_id=${row.syncId}"
            }
            statement.resultSet.use { resultSet ->
                check(resultSet.next()) {
                    "Conditional mirror upsert lost row: " +
                        "table=${codec.table.tableName}, sync_id=${row.syncId}"
                }
                return codec.read(resultSet)
            }
        }
    }

    private companion object {
        val LOGGER: java.util.logging.Logger = java.util.logging.Logger.getLogger("MirrorSync")
    }
}
