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
 * Mobile JDBC gateway к typed mirror tables в YDB.
 */
class MobileYdbMirrorGateway(
    private val config: MobileYdbConfig,
    private val serviceAccountJson: String?,
) {
    /**
     * Проверяет доступ к YDB и создаёт mobile tables, если их ещё нет.
     */
    fun status(): MobileSyncStatus {
        return runCatching {
            withConnection { connection ->
                mobileCodecs.values.forEach { codec -> ensureTable(connection, codec) }
            }
            MobileSyncStatus(
                configured = true,
                checkedAt = getCurrentLocalDateTime(),
            )
        }.getOrElse { throwable ->
            MobileSyncStatus(
                configured = true,
                checkedAt = getCurrentLocalDateTime(),
                error = throwable.message ?: "YDB недоступна",
            )
        }
    }

    /**
     * Загружает полный remote snapshot для таблиц, нужных Android-приложению.
     */
    fun loadSnapshot(): Result<MobileMirrorSnapshot> = runCatching {
        val rows = withConnection { connection ->
            mobileCodecs.values.associate { codec ->
                ensureTable(connection, codec)
                codec.table to loadRows(connection, codec)
            }
        }
        MobileMirrorSnapshot(
            loadedAt = getCurrentLocalDateTime(),
            rowsByTable = rows,
        )
    }

    /**
     * Отправляет выбранные local winners в remote mirror через batch UPSERT.
     */
    fun push(changes: List<MobileMirrorChange>): Result<Unit> = runCatching {
        withConnection { connection ->
            changes.groupBy(MobileMirrorChange::table).forEach { (table, tableChanges) ->
                val codec = mobileCodecs.getValue(table)
                ensureTable(connection, codec)
                connection.prepareStatement(codec.upsertSql(tablePath(table))).use { statement ->
                    tableChanges.forEach { change ->
                        codec.bind(statement, change.row)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
            }
        }
    }

    private fun ensureTable(connection: Connection, codec: MobileYdbCodec) {
        connection.createStatement().use { statement ->
            statement.execute(codec.createTableSql(tablePath(codec.table)))
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
 * Typed codec между mobile mirror row и строкой YDB.
 */
private interface MobileYdbCodec {
    val table: MobileMirrorTable
    val columns: List<String>
    val createColumnsSql: String

    /** Заполняет JDBC parameters для UPSERT. */
    fun bind(statement: PreparedStatement, row: MobileMirrorRow)

    /** Читает одну строку YDB ResultSet в typed mobile row. */
    fun read(resultSet: ResultSet): MobileMirrorRow

    /** Строит DDL для typed mirror table. */
    fun createTableSql(tablePath: String): String {
        return """
            CREATE TABLE IF NOT EXISTS `$tablePath` (
                $createColumnsSql,
                PRIMARY KEY (sync_id)
            )
        """.trimIndent()
    }

    /** Строит SELECT всех колонок codec-а. */
    fun selectAllSql(tablePath: String): String {
        return "SELECT ${columns.joinToString()} FROM `$tablePath`"
    }

    /** Строит typed UPSERT с явными YDB CAST для JDBC parameters. */
    fun upsertSql(tablePath: String): String {
        val values = columns.joinToString { "CAST(? AS ${columnType(it)})" }
        return """
            UPSERT INTO `$tablePath` (${columns.joinToString()})
            VALUES ($values)
        """.trimIndent()
    }
}

private object ExperimentCodec : MobileYdbCodec {
    override val table = MobileMirrorTable.EXPERIMENT
    override val columns = listOf("sync_id", "title", "idea_description", "is_archived", "updated_at", "deleted_at")
    override val createColumnsSql = """
        sync_id Utf8,
        title Utf8,
        idea_description Utf8,
        is_archived Bool,
        updated_at Utf8,
        deleted_at Utf8
    """.trimIndent()

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
    override val createColumnsSql = """
        sync_id Utf8,
        experiment_sync_id Utf8,
        entry_date Utf8,
        created_at Utf8,
        content Utf8,
        updated_at Utf8,
        deleted_at Utf8
    """.trimIndent()

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
    override val createColumnsSql = """
        sync_id Utf8,
        experiment_sync_id Utf8,
        text Utf8,
        reminder_date_time Utf8,
        updated_at Utf8,
        deleted_at Utf8
    """.trimIndent()

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
    override val createColumnsSql = """
        sync_id Utf8,
        owner_type Utf8,
        owner_sync_id Utf8,
        display_name Utf8,
        path Utf8,
        remote_object_key Utf8,
        remote_storage_provider Utf8,
        updated_at Utf8,
        deleted_at Utf8
    """.trimIndent()

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
