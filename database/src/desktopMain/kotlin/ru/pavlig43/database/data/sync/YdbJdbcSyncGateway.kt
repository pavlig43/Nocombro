package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

class YdbJdbcSyncGateway(
    private val config: YdbJdbcConfig,
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
                error = it.message,
            )
        }
    }

    override suspend fun pushChanges(
        payload: RemotePushPayload,
    ): Result<RemotePushResult> {
        return runCatching {
            withConnection { connection ->
                ensureSyncTable(connection)
                upsertChanges(
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

    private fun <T> withConnection(
        block: (Connection) -> T,
    ): T {
        val properties = Properties().apply {
            config.authToken
                ?.takeIf(String::isNotBlank)
                ?.let { setProperty("token", it) }
        }

        return DriverManager.getConnection(config.jdbcUrl, properties).use(block)
    }

    private fun parseYdbTimestamp(
        rawValue: String,
    ): LocalDateTime {
        return LocalDateTime.parse(rawValue.removeSuffix("Z"))
    }
}
