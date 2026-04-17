package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.pavlig43.database.data.files.FILE_TABLE_NAME
import java.io.FileNotFoundException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLInvalidAuthorizationSpecException
import java.sql.SQLException
import java.util.Properties
import javax.net.ssl.SSLException

class YdbJdbcSyncGateway(
    private val config: YdbJdbcConfig,
    private val json: Json = Json {
        ignoreUnknownKeys = true
    },
) : SyncRemoteGateway {

    override suspend fun getStatus(syncState: SyncStateEntity?): RemoteSyncStatus {
        return runCatching {
            withConnection { connection ->
                ensureSyncTable(connection)
                RemoteSyncStatus(
                    configured = true,
                    hasRemoteChanges = countRemoteChanges(connection, syncState) > 0,
                    remoteCursor = syncState?.lastRemoteCursor,
                )
            }
        }.getOrElse {
            RemoteSyncStatus(
                configured = false,
                hasRemoteChanges = false,
                remoteCursor = syncState?.lastRemoteCursor,
                error = it.toUiMessage(config),
            )
        }
    }

    override suspend fun loadCurrentRemoteFileStates(): Result<List<RemoteFileSyncState>> {
        return runCatching {
            withConnection { connection ->
                ensureSyncTable(connection)
                loadCurrentRemoteFileStates(connection)
            }
        }
    }

    override suspend fun pushChanges(
        payload: RemotePushPayload,
    ): Result<RemotePushResult> {
        return runCatching {
            withConnection { connection ->
                ensureSyncTable(connection)
                ensureReminderSourceTable(connection)
                upsertChanges(
                    connection = connection,
                    payload = payload,
                )
                upsertReminderSource(
                    connection = connection,
                    payload = payload,
                )
            }

            RemotePushResult(
                pushedAt = defaultUpdatedAt(),
                remoteCursor = payload.lastRemoteCursor,
            )
        }
    }

    override suspend fun pullChanges(
        deviceId: String,
        lastRemoteCursor: String?,
        limit: Int,
    ): Result<RemotePullResult> {
        return runCatching {
            withConnection { connection ->
                ensureSyncTable(connection)
                ensureReminderSourceTable(connection)
                val changes = loadRemoteChanges(
                    connection = connection,
                    deviceId = deviceId,
                    lastRemoteCursor = lastRemoteCursor,
                    limit = limit,
                )
                RemotePullResult(
                    changes = changes,
                    pulledAt = defaultUpdatedAt(),
                    remoteCursor = changes.lastOrNull()?.cursor ?: lastRemoteCursor,
                )
            }
        }
    }

    private fun upsertChanges(
        connection: Connection,
        payload: RemotePushPayload,
    ) {
        val sql = """
            UPSERT INTO `${config.tablePath}` (
                device_id,
                entity_table,
                entity_sync_id,
                change_type,
                payload_json,
                source_queue_ids,
                last_queued_at,
                reserved_at,
                pushed_at,
                remote_cursor,
                change_cursor
            ) VALUES (
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(CurrentUtcTimestamp() AS Utf8),
                CAST(? AS Utf8),
                CAST(
                    CAST(CurrentUtcTimestamp() AS Utf8) || '|' || CAST(? AS Utf8) || '|' || CAST(? AS Utf8)
                    AS Utf8
                )
            )
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            payload.changes.forEach { change ->
                statement.setString(1, payload.deviceId)
                statement.setString(2, change.entityTable)
                statement.setString(3, change.entityLocalId)
                statement.setString(4, change.changeType.name)
                statement.setString(5, change.payloadJson)
                statement.setString(6, change.sourceQueueIds.joinToString(","))
                statement.setString(7, change.lastQueuedAt.toString())
                statement.setString(8, payload.reservedAt.toString())
                statement.setString(9, payload.lastRemoteCursor ?: "")
                statement.setString(10, change.entityTable)
                statement.setString(11, change.entityLocalId)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun ensureSyncTable(
        connection: Connection,
    ) {
        val sql = """
            CREATE TABLE IF NOT EXISTS `${config.tablePath}` (
                device_id Utf8,
                entity_table Utf8,
                entity_sync_id Utf8,
                change_type Utf8,
                payload_json Utf8,
                source_queue_ids Utf8,
                last_queued_at Utf8,
                reserved_at Utf8,
                pushed_at Utf8,
                remote_cursor Utf8,
                change_cursor Utf8,
                PRIMARY KEY (entity_table, entity_sync_id)
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    private fun ensureReminderSourceTable(
        connection: Connection,
    ) {
        val sql = """
            CREATE TABLE IF NOT EXISTS `${config.reminderSourceTablePath}` (
                reminder_sync_id Utf8,
                transaction_sync_id Utf8,
                transaction_type Utf8,
                transaction_created_at Utf8,
                reminder_text Utf8,
                reminder_at Utf8,
                updated_at Utf8,
                deleted_at Utf8,
                PRIMARY KEY (reminder_sync_id)
            )
        """.trimIndent()

        connection.createStatement().use { statement ->
            statement.execute(sql)
        }
    }

    private fun upsertReminderSource(
        connection: Connection,
        payload: RemotePushPayload,
    ) {
        val reminderChanges = payload.changes.mapNotNull { it.reminderEmailSource }
        if (reminderChanges.isEmpty()) {
            return
        }

        val sql = """
            UPSERT INTO `${config.reminderSourceTablePath}` (
                reminder_sync_id,
                transaction_sync_id,
                transaction_type,
                transaction_created_at,
                reminder_text,
                reminder_at,
                updated_at,
                deleted_at
            ) VALUES (
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8),
                CAST(? AS Utf8)
            )
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            reminderChanges.forEach { change ->
                statement.setString(1, change.reminderSyncId)
                statement.setString(2, change.transactionSyncId)
                statement.setString(3, change.transactionType)
                statement.setString(4, change.transactionCreatedAt.toString())
                statement.setString(5, change.reminderText)
                statement.setString(6, change.reminderAt?.toString())
                statement.setString(7, change.updatedAt.toString())
                statement.setString(8, change.deletedAt?.toString())
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun countRemoteChanges(
        connection: Connection,
        syncState: SyncStateEntity?,
    ): Int {
        val sql = buildString {
            append("SELECT COUNT(*) FROM `")
            append(config.tablePath)
            append("` WHERE device_id <> ?")
            if (!syncState?.lastRemoteCursor.isNullOrBlank()) {
                append(" AND change_cursor > ?")
            }
        }

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, syncState?.deviceId.orEmpty())
            if (!syncState?.lastRemoteCursor.isNullOrBlank()) {
                statement.setString(2, syncState.lastRemoteCursor)
            }
            statement.executeQuery().use { resultSet ->
                return if (resultSet.next()) resultSet.getInt(1) else 0
            }
        }
    }

    private fun loadRemoteChanges(
        connection: Connection,
        deviceId: String,
        lastRemoteCursor: String?,
        limit: Int,
    ): List<RemotePullChange> {
        val sql = buildString {
            append(
                """
                SELECT
                    change_cursor,
                    device_id,
                    entity_table,
                    entity_sync_id,
                    change_type,
                    pushed_at,
                    payload_json
                FROM `${config.tablePath}`
                WHERE device_id <> ?
                """.trimIndent()
            )
            if (!lastRemoteCursor.isNullOrBlank()) {
                append(" AND change_cursor > ?")
            }
            append(" ORDER BY change_cursor ASC LIMIT ?")
        }

        connection.prepareStatement(sql).use { statement ->
            var parameterIndex = 1
            statement.setString(parameterIndex++, deviceId)
            if (!lastRemoteCursor.isNullOrBlank()) {
                statement.setString(parameterIndex++, lastRemoteCursor)
            }
            statement.setInt(parameterIndex, limit)
            statement.executeQuery().use { resultSet ->
                val changes = mutableListOf<RemotePullChange>()
                while (resultSet.next()) {
                    changes += RemotePullChange(
                        cursor = resultSet.getString("change_cursor"),
                        sourceDeviceId = resultSet.getString("device_id"),
                        entityTable = resultSet.getString("entity_table"),
                        entitySyncId = resultSet.getString("entity_sync_id"),
                        changeType = enumValueOf(resultSet.getString("change_type")),
                        changedAt = parseYdbTimestamp(resultSet.getString("pushed_at")),
                        payloadJson = resultSet.getString("payload_json"),
                    )
                }
                return changes
            }
        }
    }

    private fun loadCurrentRemoteFileStates(
        connection: Connection,
    ): List<RemoteFileSyncState> {
        val sql = """
            SELECT
                entity_sync_id,
                change_type,
                payload_json
            FROM `${config.tablePath}`
            WHERE entity_table = ?
        """.trimIndent()

        connection.prepareStatement(sql).use { statement ->
            statement.setString(1, FILE_TABLE_NAME)
            statement.executeQuery().use { resultSet ->
                val items = mutableListOf<RemoteFileSyncState>()
                while (resultSet.next()) {
                    val syncId = resultSet.getString("entity_sync_id")
                    val changeType = enumValueOf<SyncChangeType>(resultSet.getString("change_type"))
                    val payloadJson = resultSet.getString("payload_json")
                    val payload = payloadJson
                        ?.let(::decodeFilePayload)

                    items += RemoteFileSyncState(
                        syncId = syncId,
                        remoteObjectKey = payload?.remoteObjectKey,
                        changeType = changeType,
                        deletedAt = payload?.deletedAt,
                    )
                }
                return items
            }
        }
    }

    private fun decodeFilePayload(
        rawPayload: String,
    ): RemoteFilePayloadSnapshot {
        val rootElement = json.decodeFromString<kotlinx.serialization.json.JsonElement>(rawPayload)
        val payloadElement = rootElement.jsonObject["payload"] ?: rootElement
        val payloadObject = payloadElement.jsonObject

        return RemoteFilePayloadSnapshot(
            remoteObjectKey = payloadObject.jsonStringOrNull("remoteObjectKey"),
            deletedAt = payloadObject.jsonStringOrNull("deletedAt")
                ?.let { rawDateTime -> LocalDateTime.parse(rawDateTime) },
        )
    }

    private data class RemoteFilePayloadSnapshot(
        val remoteObjectKey: String?,
        val deletedAt: LocalDateTime?,
    )

    private fun Map<String, kotlinx.serialization.json.JsonElement>.jsonStringOrNull(
        key: String,
    ): String? {
        val element = this[key] ?: return null
        return runCatching { element.jsonPrimitive.content }
            .getOrNull()
            ?.takeUnless { it == "null" }
    }

    private fun <T> withConnection(
        block: (Connection) -> T,
    ): T {
        val properties = Properties().apply {
            val serviceAccountFile = config.serviceAccountFile
                ?.takeIf(String::isNotBlank)

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

    private fun parseYdbTimestamp(
        rawValue: String,
    ): LocalDateTime {
        return LocalDateTime.parse(rawValue.removeSuffix("Z"))
    }
}

private fun Throwable.toUiMessage(
    config: YdbJdbcConfig,
): String {
    val causeChain = generateSequence(this) { it.cause }.toList()
    val rawMessage = causeChain
        .mapNotNull(Throwable::message)
        .firstOrNull(String::isNotBlank)
        .orEmpty()
    val normalizedMessage = rawMessage.lowercase()
    val serviceAccountFile = config.serviceAccountFile?.lowercase()

    if (causeChain.any { it is UnknownHostException }) {
        return "Не удалось найти хост удаленной БД. Проверьте интернет и JDBC URL."
    }
    if (causeChain.any { it is ConnectException }) {
        return "Не удалось подключиться к удаленной БД. Проверьте интернет и доступность сервера."
    }
    if (causeChain.any { it is SocketTimeoutException }) {
        return "Таймаут подключения к удаленной БД. Проверьте интернет или повторите позже."
    }
    if (causeChain.any { it is SSLException }) {
        return "Не удалось установить защищенное соединение с удаленной БД."
    }
    if (causeChain.any { it is FileNotFoundException }) {
        return "Не найден файл сервисного аккаунта для доступа к удаленной БД."
    }
    if (
        serviceAccountFile != null &&
        serviceAccountFile.isNotBlank() &&
        normalizedMessage.contains(serviceAccountFile)
    ) {
        return "Не удалось прочитать файл сервисного аккаунта для доступа к удаленной БД."
    }
    if (
        causeChain.any { it is SQLInvalidAuthorizationSpecException } ||
        normalizedMessage.contains("auth") ||
        normalizedMessage.contains("authentication") ||
        normalizedMessage.contains("unauthorized") ||
        normalizedMessage.contains("permission denied") ||
        normalizedMessage.contains("access denied") ||
        normalizedMessage.contains("token") ||
        normalizedMessage.contains("credential") ||
        normalizedMessage.contains("sakeyfile") ||
        normalizedMessage.contains("service account")
    ) {
        return "Ошибка авторизации в удаленной БД."
    }
    if (
        causeChain.any { it is SQLException } ||
        normalizedMessage.contains("jdbc") ||
        normalizedMessage.contains("driver")
    ) {
        return "Ошибка подключения к удаленной БД: ${rawMessage.ifBlank { "проверьте JDBC конфигурацию." }}"
    }

    return rawMessage.ifBlank { "Неизвестная ошибка подключения к удаленной БД." }
}
