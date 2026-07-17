package ru.pavlig43.nocombro.mobile.sync

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.util.Properties
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.datetime.getCurrentLocalDateTime

/**
 * JDBC-шлюз Android-клиента к типизированным mirror-таблицам в YDB.
 *
 * Шлюз не знает про локальную Room-БД и S3. Его задача узкая: открыть JDBC
 * соединение, убедиться, что нужные mobile-таблицы есть, прочитать строки и
 * записать выбранные строки обратно через `UPSERT`.
 */
class MobileYdbMirrorGateway(
    private val config: MobileYdbConfig,
    private val serviceAccountJson: String?,
) {
    /**
     * Проверяет доступ к уже созданным YDB-таблицам.
     *
     * Во время работы схема не меняется: если таблицы нет или схема не та,
     * пользователь увидит ошибку синхронизации.
     */
    fun status(): MobileSyncStatus {
        return runCatching {
            withConnection { connection ->
                mobileCodecs.values.forEach { codec -> checkTableReadable(connection, codec) }
            }
            MobileSyncStatus(
                configured = true,
                checkedAt = getCurrentLocalDateTime(),
            )
        }.getOrElse { throwable ->
            MobileSyncStatus(
                configured = true,
                checkedAt = getCurrentLocalDateTime(),
                error = throwable.mobileSyncErrorMessage("YDB недоступна"),
            )
        }
    }

    /**
     * Загружает полный удалённый снимок таблиц, нужных Android-приложению.
     *
     * Android работает только с экспериментами, записями, напоминаниями и файлами
     * записей. Остальные desktop-таблицы этот шлюз не читает.
     */
    fun loadSnapshot(): Result<MobileMirrorSnapshot> = runCatching {
        val rows = withConnection { connection ->
            mobileCodecs.values.associate { codec ->
                codec.table to loadRows(connection, codec)
            }
        }
        MobileMirrorSnapshot(
            loadedAt = getCurrentLocalDateTime(),
            rowsByTable = rows,
        )
    }

    /**
     * Условно отправляет выбранные локальные версии в удалённое зеркало.
     *
     * Каждая строка записывается лишь когда в YDB нет версии не старше входящей.
     * Тот же serializable DML-запрос возвращает итоговую строку, поэтому вызывающий
     * код точно знает, была запись принята или проиграла конкурентному клиенту.
     * Ошибка одной строки получает безопасный контекст таблицы и `sync_id`.
     *
     * @param changes строки, выбранные локальным планировщиком для push.
     * @return принятые и отклонённые строки с фактическими значениями из YDB.
     */
    fun push(changes: List<MobileMirrorChange>): Result<MobilePushResult> = runCatching {
        val accepted = mutableListOf<MobileMirrorChange>()
        val rejected = mutableListOf<MobilePushRejection>()
        withConnection { connection ->
            changes.forEach { change ->
                val codec = mobileCodecs.getValue(change.table)
                val remoteRow = runCatching {
                    conditionalUpsertRow(
                        connection = connection,
                        codec = codec,
                        tablePath = tablePath(change.table),
                        row = change.row,
                    )
                }.getOrElse { throwable ->
                    throw MobileSyncOperationException(
                        message = "table=${change.table.tableName}, sync_id=${change.row.syncId}",
                        cause = throwable,
                    )
                }
                if (remoteRow.hasSameSyncContent(change.row)) {
                    accepted += change
                } else {
                    rejected += MobilePushRejection(
                        change = change,
                        remoteRow = remoteRow,
                        equalVersionConflict = remoteRow.versionAt() == change.row.versionAt(),
                    )
                }
            }
        }
        MobilePushResult(accepted, rejected)
    }

    /**
     * Выполняет условный `UPSERT` и читает строку-победителя из того же запроса.
     *
     * @return входная строка после успешной записи либо более новая строка YDB.
     * @throws IllegalStateException если YDB не вернула итоговый набор или строку.
     */
    private fun conditionalUpsertRow(
        connection: Connection,
        codec: MobileYdbCodec,
        tablePath: String,
        row: MobileMirrorRow,
    ): MobileMirrorRow {
        connection.prepareStatement(codec.conditionalUpsertSql(tablePath)).use { statement ->
            codec.bind(statement, row)
            var parameterIndex = codec.columns.size + 1
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

    private fun checkTableReadable(connection: Connection, codec: MobileYdbCodec) {
        connection.createStatement().use { statement ->
            statement.executeQuery(codec.selectProbeSql(tablePath(codec.table))).use { }
        }
    }

    private fun loadRows(
        connection: Connection,
        codec: MobileYdbCodec,
    ): List<MobileMirrorRow> {
        connection.createStatement().use { statement ->
            statement.executeQuery(codec.selectAllSql(tablePath(codec.table))).use { resultSet ->
                return buildList {
                    while (resultSet.next()) {
                        add(codec.read(resultSet))
                    }
                }
            }
        }
    }

    private fun tablePath(table: MobileMirrorTable): String {
        val root = config.tableRoot.trim().trim('/')
        return if (root.isEmpty()) table.tableName else "$root/${table.tableName}"
    }

    private fun <T> withConnection(block: (Connection) -> T): T {
        val properties = Properties().apply {
            serviceAccountJson?.takeIf(String::isNotBlank)?.let {
                put("tokenProvider", MobileIamTokenSupplier(it))
            }
                ?: config.token?.takeIf(String::isNotBlank)?.let { setProperty("token", it) }
        }
        return DriverManager.getConnection(config.jdbcUrl, properties).use(block)
    }
}

/**
 * Кодек между строкой mobile mirror и строкой YDB.
 *
 * Каждый кодек знает только одну таблицу: её DDL, список колонок, чтение из
 * `ResultSet` и привязку параметров для `UPSERT`.
 */
private interface MobileYdbCodec {
    val table: MobileMirrorTable
    val columns: List<String>

    /** Заполняет JDBC-параметры для `UPSERT`. */
    fun bind(statement: PreparedStatement, row: MobileMirrorRow)

    /** Читает одну строку YDB `ResultSet` в mobile-строку. */
    fun read(resultSet: ResultSet): MobileMirrorRow

    /** Строит `SELECT` всех колонок кодека. */
    fun selectAllSql(tablePath: String): String {
        return "SELECT ${columns.joinToString()} FROM `$tablePath`"
    }

    /** Строит `SELECT` без чтения строк для проверки таблицы и схемы. */
    fun selectProbeSql(tablePath: String): String {
        return "${selectAllSql(tablePath)} LIMIT 0"
    }

    /** Строит типизированный `UPSERT` с явными YDB `CAST` для JDBC-параметров. */
    fun upsertSql(tablePath: String): String {
        val values = columns.joinToString { "CAST(? AS ${columnType(it)})" }
        return """
            UPSERT INTO `$tablePath` (${columns.joinToString()})
            VALUES ($values)
        """.trimIndent()
    }

    /**
     * Строит serializable DML для условной записи и чтения итога.
     *
     * `UPSERT` блокируется при равной или более новой фактической версии YDB.
     * Следующий `SELECT` возвращает строку-победителя независимо от исхода записи,
     * что позволяет отличить принятую строку от конфликта без окна гонки.
     *
     * @param tablePath полный путь mirror-таблицы в YDB.
     */
    fun conditionalUpsertSql(tablePath: String): String {
        return mobileConditionalUpsertSql(columns, tablePath)
    }
}

/**
 * Builds the YDB-specific conditional upsert used by the Android JDBC client.
 *
 * YDB requires a row source when a `SELECT` containing JDBC parameters is filtered by
 * `WHERE`. The one-row `AS_TABLE` source keeps the operation conditional without changing
 * the values being written.
 */
internal fun mobileConditionalUpsertSql(
    columns: List<String>,
    tablePath: String,
): String {
    val values = columns.joinToString { "CAST(? AS ${columnType(it)})" }
    return """
        UPSERT INTO `$tablePath` (${columns.joinToString()})
        SELECT $values
        FROM AS_TABLE(AsList(AsStruct(1 AS _source)))
        WHERE NOT EXISTS (
            SELECT sync_id FROM `$tablePath`
            WHERE sync_id = CAST(? AS Utf8)
              AND IF(
                  deleted_at IS NOT NULL AND deleted_at > updated_at,
                  deleted_at,
                  updated_at
              ) >= CAST(? AS Utf8)
        );
        SELECT ${columns.joinToString()} FROM `$tablePath`
        WHERE sync_id = CAST(? AS Utf8);
    """.trimIndent()
}

private object ExperimentCodec : MobileYdbCodec {
    override val table = MobileMirrorTable.EXPERIMENT
    override val columns = listOf("sync_id", "title", "idea_description", "is_archived", "updated_at", "deleted_at")

    override fun bind(statement: PreparedStatement, row: MobileMirrorRow) {
        require(row is MobileExperimentMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.title)
        statement.setString(3, row.ideaDescription)
        statement.setBoolean(4, row.isArchived)
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = MobileExperimentMirrorRow(
        syncId = resultSet.getString("sync_id"),
        title = resultSet.getString("title"),
        ideaDescription = resultSet.getString("idea_description"),
        isArchived = resultSet.getBoolean("is_archived"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

private object ExperimentEntryCodec : MobileYdbCodec {
    override val table = MobileMirrorTable.EXPERIMENT_ENTRY
    override val columns = listOf(
        "sync_id",
        "experiment_sync_id",
        "entry_date",
        "created_at",
        "content",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MobileMirrorRow) {
        require(row is MobileExperimentEntryMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.experimentSyncId)
        statement.setString(3, row.entryDate.toString())
        statement.setString(4, row.createdAt.toString())
        statement.setString(5, row.content)
        statement.setString(6, row.updatedAt.toString())
        statement.setString(7, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = MobileExperimentEntryMirrorRow(
        syncId = resultSet.getString("sync_id"),
        experimentSyncId = resultSet.getString("experiment_sync_id"),
        entryDate = LocalDate.parse(resultSet.getString("entry_date")),
        createdAt = resultSet.dateTime("created_at"),
        content = resultSet.getString("content"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

private object ExperimentReminderCodec : MobileYdbCodec {
    override val table = MobileMirrorTable.EXPERIMENT_REMINDER
    override val columns = listOf(
        "sync_id",
        "experiment_sync_id",
        "text",
        "reminder_date_time",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MobileMirrorRow) {
        require(row is MobileExperimentReminderMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.experimentSyncId)
        statement.setString(3, row.text)
        statement.setString(4, row.reminderDateTime.toString())
        statement.setString(5, row.updatedAt.toString())
        statement.setString(6, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = MobileExperimentReminderMirrorRow(
        syncId = resultSet.getString("sync_id"),
        experimentSyncId = resultSet.getString("experiment_sync_id"),
        text = resultSet.getString("text"),
        reminderDateTime = resultSet.dateTime("reminder_date_time"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

private object FileCodec : MobileYdbCodec {
    override val table = MobileMirrorTable.FILE
    override val columns = listOf(
        "sync_id",
        "owner_type",
        "owner_sync_id",
        "display_name",
        "path",
        "remote_object_key",
        "remote_storage_provider",
        "updated_at",
        "deleted_at",
    )

    override fun bind(statement: PreparedStatement, row: MobileMirrorRow) {
        require(row is MobileFileMirrorRow)
        statement.setString(1, row.syncId)
        statement.setString(2, row.ownerType.name)
        statement.setString(3, row.ownerSyncId)
        statement.setString(4, row.displayName)
        statement.setString(5, row.path)
        statement.setString(6, row.remoteObjectKey)
        statement.setString(7, row.remoteStorageProvider)
        statement.setString(8, row.updatedAt.toString())
        statement.setString(9, row.deletedAt?.toString())
    }

    override fun read(resultSet: ResultSet) = MobileFileMirrorRow(
        syncId = resultSet.getString("sync_id"),
        ownerType = enumValueOf<MobileFileOwnerType>(resultSet.getString("owner_type")),
        ownerSyncId = resultSet.getString("owner_sync_id"),
        displayName = resultSet.getString("display_name"),
        path = resultSet.getString("path"),
        remoteObjectKey = resultSet.getString("remote_object_key"),
        remoteStorageProvider = resultSet.getString("remote_storage_provider"),
        updatedAt = resultSet.dateTime("updated_at"),
        deletedAt = resultSet.nullableDateTime("deleted_at"),
    )
}

private val mobileCodecs = listOf(
    ExperimentCodec,
    ExperimentEntryCodec,
    ExperimentReminderCodec,
    FileCodec,
).associateBy(MobileYdbCodec::table)

private fun ResultSet.dateTime(column: String): LocalDateTime = LocalDateTime.parse(getString(column))

private fun ResultSet.nullableDateTime(column: String): LocalDateTime? = getString(column)?.let(LocalDateTime::parse)

private fun columnType(columnName: String): String {
    return if (columnName == "is_archived") "Bool" else "Utf8"
}
