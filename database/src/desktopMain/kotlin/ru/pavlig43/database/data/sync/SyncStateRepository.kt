package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime

/**
 * Доступ к техническому состоянию синхронизации.
 */
class SyncStateRepository(
    private val syncDao: SyncDao,
) {

    suspend fun getSyncState(
        scope: String = DEFAULT_SYNC_SCOPE,
    ): SyncStateEntity? = syncDao.getSyncState(scope)

    suspend fun updateLastPushAt(
        pushedAt: LocalDateTime,
        scope: String = DEFAULT_SYNC_SCOPE,
        remoteCursor: String? = null,
    ) {
        val currentState = syncDao.getSyncState(scope)
            ?: error("Sync state for scope `$scope` is not initialized")

        syncDao.upsertSyncState(
            currentState.copy(
                lastPushAt = pushedAt,
                lastRemoteCursor = remoteCursor ?: currentState.lastRemoteCursor,
            )
        )
    }

    suspend fun updateLastPullAt(
        pulledAt: LocalDateTime,
        scope: String = DEFAULT_SYNC_SCOPE,
        remoteCursor: String? = null,
    ) {
        val currentState = syncDao.getSyncState(scope)
            ?: error("Sync state for scope `$scope` is not initialized")

        syncDao.upsertSyncState(
            currentState.copy(
                lastPullAt = pulledAt,
                lastRemoteCursor = remoteCursor ?: currentState.lastRemoteCursor,
            )
        )
    }
}
