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
            configured = false,
            hasRemoteChanges = false,
            remoteCursor = syncState?.lastRemoteCursor,
            error = "Remote sync не настроен.",
        )
    }

    override suspend fun loadCurrentRemoteFileStates(): Result<List<RemoteFileSyncState>> {
        return Result.failure(
            IllegalStateException("Remote sync не настроен.")
        )
    }

    override suspend fun pushChanges(
        payload: RemotePushPayload,
    ): Result<RemotePushResult> {
        return Result.failure(
            IllegalStateException("Remote sync не настроен.")
        )
    }

    override suspend fun pullChanges(
        deviceId: String,
        lastRemoteCursor: String?,
        limit: Int,
    ): Result<RemotePullResult> {
        return Result.failure(
            IllegalStateException("Remote sync не настроен.")
        )
    }
}
