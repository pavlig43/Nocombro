package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.inTransaction

/**
 * Локальный раннер очереди синхронизации.
 *
 * Пока он не ходит в сеть и не знает про конкретный серверный API.
 * Его задача - безопасно забрать пачку pending-изменений, подготовить их
 * к отправке и затем либо подтвердить, либо вернуть с ошибкой.
 */
class SyncRunner(
    private val db: NocombroDatabase,
    private val syncQueueRepository: SyncQueueRepository,
) {

    /**
     * Забирает из очереди пачку изменений и помечает исходные записи как `IN_PROGRESS`.
     *
     * Внутри пачки повторные изменения одной и той же сущности схлопываются
     * до одного действия с самым свежим состоянием.
     */
    suspend fun reservePendingBatch(
        limit: Int = 100,
        reservedAt: LocalDateTime = defaultUpdatedAt(),
    ): Result<SyncPushBatch?> {
        return runCatching {
            db.inTransaction {
                val pendingChanges = syncQueueRepository.getChanges(limit = limit)
                if (pendingChanges.isEmpty()) {
                    return@inTransaction null
                }

                val queueIds = pendingChanges.map(SyncChangeEntity::id)
                syncQueueRepository.markInProgress(ids = queueIds, updatedAt = reservedAt)

                buildPushBatch(pendingChanges, reservedAt)
            }
        }
    }

    /**
     * Подтверждает успешную обработку пачки и удаляет ее исходные элементы из очереди.
     */
    suspend fun markBatchSucceeded(
        batch: SyncPushBatch,
    ) {
        syncQueueRepository.deleteProcessed(batch.queueIds)
    }

    /**
     * Помечает всю пачку как завершившуюся ошибкой.
     */
    suspend fun markBatchFailed(
        batch: SyncPushBatch,
        error: String?,
        failedAt: LocalDateTime = defaultUpdatedAt(),
    ) {
        syncQueueRepository.markFailed(
            ids = batch.queueIds,
            error = error,
            updatedAt = failedAt,
        )
    }
}

/**
 * Подготовленная пачка локальных изменений для будущего push на сервер.
 */
data class SyncPushBatch(
    /**
     * Исходные id элементов очереди, которые были зарезервированы под эту пачку.
     */
    val queueIds: List<Long>,

    /**
     * Дедуплицированные изменения, которые уже можно преобразовывать в серверный payload.
     */
    val changes: List<SyncPushChange>,

    /**
     * Время, когда пачка была зарезервирована на отправку.
     */
    val reservedAt: LocalDateTime,
)

/**
 * Одно подготовленное изменение для синхронизации с сервером.
 */
data class SyncPushChange(
    val entityTable: String,
    val entityLocalId: String,
    val changeType: SyncChangeType,
    val sourceQueueIds: List<Long>,
    val lastQueuedAt: LocalDateTime,
)

private fun buildPushBatch(
    pendingChanges: List<SyncChangeEntity>,
    reservedAt: LocalDateTime,
): SyncPushBatch {
    val preparedChanges = pendingChanges
        .groupBy { change -> change.entityTable to change.entityLocalId }
        .values
        .map { entityChanges ->
            val latestChange = entityChanges.maxWith(
                compareBy<SyncChangeEntity>({ it.createdAt }, { it.id })
            )

            SyncPushChange(
                entityTable = latestChange.entityTable,
                entityLocalId = latestChange.entityLocalId,
                changeType = latestChange.changeType,
                sourceQueueIds = entityChanges.map(SyncChangeEntity::id),
                lastQueuedAt = latestChange.createdAt,
            )
        }
        .sortedWith(compareBy<SyncPushChange>({ it.lastQueuedAt }, { it.entityTable }, { it.entityLocalId }))

    return SyncPushBatch(
        queueIds = pendingChanges.map(SyncChangeEntity::id),
        changes = preparedChanges,
        reservedAt = reservedAt,
    )
}
