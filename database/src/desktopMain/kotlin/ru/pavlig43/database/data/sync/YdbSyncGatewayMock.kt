package ru.pavlig43.database.data.sync

/**
 * Временный adapter под будущую интеграцию с YDB.
 *
 * Сейчас он помечает удаленную синхронизацию как настроенную и успешно
 * принимает подготовленные batch-изменения без реального сетевого вызова.
 */
class YdbSyncGatewayMock : SyncRemoteGateway {

    override suspend fun getStatus(syncState: SyncStateEntity?): RemoteSyncStatus {
        return RemoteSyncStatus(
            configured = true,
            hasRemoteChanges = false,
            remoteCursor = syncState?.lastRemoteCursor,
        )
    }

    override suspend fun loadCurrentRemoteFileStates(): Result<List<RemoteFileSyncState>> {
        return Result.success(emptyList())
    }

    override suspend fun pushChanges(
        payload: RemotePushPayload,
    ): Result<RemotePushResult> {
        return Result.success(
            RemotePushResult(
                pushedAt = defaultUpdatedAt(),
                remoteCursor = payload.lastRemoteCursor,
            )
        )
    }

    override suspend fun pullChanges(
        deviceId: String,
        lastRemoteCursor: String?,
        limit: Int,
    ): Result<RemotePullResult> {
        return Result.success(
            RemotePullResult(
                changes = emptyList(),
                pulledAt = defaultUpdatedAt(),
                remoteCursor = lastRemoteCursor,
            )
        )
    }
}
