package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime

/**
 * Небольшой сервис вокруг локальной очереди синхронизации.
 *
 * Он нужен, чтобы остальной код работал не с сырыми SQL-методами `SyncDao`,
 * а с более прикладными операциями очереди: добавить изменение, взять пачку,
 * пометить как отправляемые, вернуть в pending или зафиксировать ошибку.
 */
class SyncQueueRepository(
    private val syncDao: SyncDao,
) {

    /**
     * Добавляет в очередь изменение типа upsert для указанной локальной записи.
     */
    suspend fun enqueueUpsert(
        entityTable: String,
        entityLocalId: String,
        createdAt: LocalDateTime = defaultUpdatedAt(),
    ) {
        syncDao.enqueueChanges(
            listOf(
                SyncChangeEntity(
                    entityTable = entityTable,
                    entityLocalId = entityLocalId,
                    changeType = SyncChangeType.UPSERT,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )
            )
        )
    }

    /**
     * Добавляет в очередь изменение типа delete для указанной локальной записи.
     */
    suspend fun enqueueDelete(
        entityTable: String,
        entityLocalId: String,
        createdAt: LocalDateTime = defaultUpdatedAt(),
    ) {
        syncDao.enqueueChanges(
            listOf(
                SyncChangeEntity(
                    entityTable = entityTable,
                    entityLocalId = entityLocalId,
                    changeType = SyncChangeType.DELETE,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )
            )
        )
    }

    /**
     * Возвращает пачку изменений в указанном статусе.
     */
    suspend fun getChanges(
        status: SyncQueueStatus = SyncQueueStatus.PENDING,
        limit: Int = 100,
    ): List<SyncChangeEntity> = syncDao.getChangesByStatus(status = status, limit = limit)

    /**
     * Возвращает количество элементов очереди в выбранном статусе.
     */
    suspend fun getChangesCount(
        status: SyncQueueStatus,
    ): Int = syncDao.getChangesCountByStatus(status)

    /**
     * Помечает изменения как находящиеся в отправке.
     */
    suspend fun markInProgress(
        ids: List<Long>,
        updatedAt: LocalDateTime = defaultUpdatedAt(),
    ) {
        if (ids.isEmpty()) return
        syncDao.markChangesInProgress(ids = ids, updatedAt = updatedAt)
    }

    /**
     * Возвращает изменения обратно в очередь ожидания.
     */
    suspend fun markPending(
        ids: List<Long>,
        updatedAt: LocalDateTime = defaultUpdatedAt(),
    ) {
        if (ids.isEmpty()) return
        syncDao.markChangesPending(ids = ids, updatedAt = updatedAt)
    }

    /**
     * Помечает изменения как завершившиеся ошибкой и сохраняет текст ошибки.
     */
    suspend fun markFailed(
        ids: List<Long>,
        error: String?,
        updatedAt: LocalDateTime = defaultUpdatedAt(),
    ) {
        if (ids.isEmpty()) return
        syncDao.markChangesFailed(ids = ids, updatedAt = updatedAt, error = error)
    }

    /**
     * Удаляет успешно обработанные элементы очереди.
     */
    suspend fun deleteProcessed(ids: List<Long>) {
        if (ids.isEmpty()) return
        syncDao.deleteChanges(ids)
    }
}
