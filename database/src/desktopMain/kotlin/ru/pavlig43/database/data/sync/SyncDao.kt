package ru.pavlig43.database.data.sync

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/**
 * DAO для служебных таблиц синхронизации.
 *
 * Нужен для хранения состояния pull/push и управления локальной очередью изменений.
 */
@Dao
interface SyncDao {
    /**
     * Создает или обновляет состояние синхронизации текущей локальной базы.
     */
    @Upsert
    suspend fun upsertSyncState(syncState: SyncStateEntity)

    /**
     * Возвращает текущее состояние синхронизации для указанного scope.
     */
    @Query("SELECT * FROM sync_state WHERE scope = :scope")
    suspend fun getSyncState(scope: String = DEFAULT_SYNC_SCOPE): SyncStateEntity?

    /**
     * Добавляет изменения в локальную очередь на отправку.
     */
    @Upsert
    suspend fun enqueueChanges(changes: List<SyncChangeEntity>)

    /**
     * Возвращает изменения с заданным статусом.
     *
     * Лимит нужен, чтобы разбирать очередь батчами и не обрабатывать весь хвост за один запрос.
     */
    @Query(
        """
        SELECT * FROM sync_change
        WHERE status = :status
        ORDER BY created_at ASC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getChangesByStatus(
        status: SyncQueueStatus = SyncQueueStatus.PENDING,
        limit: Int = 100,
    ): List<SyncChangeEntity>

    /**
     * Возвращает количество элементов очереди в указанном статусе.
     */
    @Query("SELECT COUNT(*) FROM sync_change WHERE status = :status")
    suspend fun getChangesCountByStatus(
        status: SyncQueueStatus,
    ): Int

    /**
     * Помечает выбранные элементы очереди как отправляемые.
     */
    @Query(
        """
        UPDATE sync_change
        SET status = 'IN_PROGRESS',
            updated_at = :updatedAt,
            last_error = NULL
        WHERE id IN (:ids)
        """
    )
    suspend fun markChangesInProgress(
        ids: List<Long>,
        updatedAt: kotlinx.datetime.LocalDateTime,
    )

    /**
     * Возвращает элементы очереди обратно в состояние ожидания.
     */
    @Query(
        """
        UPDATE sync_change
        SET status = 'PENDING',
            updated_at = :updatedAt,
            last_error = NULL
        WHERE id IN (:ids)
        """
    )
    suspend fun markChangesPending(
        ids: List<Long>,
        updatedAt: kotlinx.datetime.LocalDateTime,
    )

    /**
     * Помечает элементы очереди как завершившиеся ошибкой.
     */
    @Query(
        """
        UPDATE sync_change
        SET status = 'FAILED',
            updated_at = :updatedAt,
            attempt_count = attempt_count + 1,
            last_error = :error
        WHERE id IN (:ids)
        """
    )
    suspend fun markChangesFailed(
        ids: List<Long>,
        updatedAt: kotlinx.datetime.LocalDateTime,
        error: String?,
    )

    /**
     * Удаляет обработанные элементы очереди.
     */
    @Query("DELETE FROM sync_change WHERE id IN (:ids)")
    suspend fun deleteChanges(ids: List<Long>)
}
