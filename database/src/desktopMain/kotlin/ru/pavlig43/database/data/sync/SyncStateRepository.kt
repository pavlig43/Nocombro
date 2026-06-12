package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime

/**
 * Доступ к техническому состоянию синхронизации.
 */
class SyncStateRepository(
    private val syncStateDao: SyncStateDao,
) {

    suspend fun getSyncState(
        scope: String = DEFAULT_SYNC_SCOPE,
    ): SyncStateEntity? = syncStateDao.getSyncState(scope)

    suspend fun updateLastPushAt(
        pushedAt: LocalDateTime,
        scope: String = DEFAULT_SYNC_SCOPE,
    ) {
        val currentState = syncStateDao.getSyncState(scope)
            ?: error("Sync state for scope `$scope` is not initialized")

        syncStateDao.upsertSyncState(
            currentState.copy(
                lastPushAt = pushedAt,
            )
        )
    }

    suspend fun updateLastPullAt(
        pulledAt: LocalDateTime,
        scope: String = DEFAULT_SYNC_SCOPE,
    ) {
        val currentState = syncStateDao.getSyncState(scope)
            ?: error("Sync state for scope `$scope` is not initialized")

        syncStateDao.upsertSyncState(
            currentState.copy(
                lastPullAt = pulledAt,
            )
        )
    }
}
