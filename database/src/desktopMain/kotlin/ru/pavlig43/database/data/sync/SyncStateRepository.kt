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

    /**
     * Записывает время успешного push, не требуя заранее созданной строки scope.
     *
     * @param pushedAt отметка завершённой отправки.
     * @param scope независимая область состояния синхронизации.
     */
    suspend fun updateLastPushAt(
        pushedAt: LocalDateTime,
        scope: String = DEFAULT_SYNC_SCOPE,
    ) {
        syncStateDao.upsertLastPushAt(scope, pushedAt)
    }

    /**
     * Записывает время успешного pull, не меняя сохранённую отметку push.
     *
     * @param pulledAt отметка завершённого получения.
     * @param scope независимая область состояния синхронизации.
     */
    suspend fun updateLastPullAt(
        pulledAt: LocalDateTime,
        scope: String = DEFAULT_SYNC_SCOPE,
    ) {
        syncStateDao.upsertLastPullAt(scope, pulledAt)
    }
}
