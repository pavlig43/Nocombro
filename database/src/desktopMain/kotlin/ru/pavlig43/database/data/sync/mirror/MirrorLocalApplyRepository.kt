package ru.pavlig43.database.data.sync.mirror

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.inTransaction

/**
 * Атомарно применяет remote winners к локальной Room-базе.
 *
 * До изменения бизнес-таблиц remote tombstone сохраняются в deletion journal.
 * Upsert сортируются от родителей к детям, delete в обратном порядке. Это сохраняет
 * внешние ключи и гарантирует, что физическое отсутствие строки не потеряет факт
 * удаления при следующем local snapshot.
 */
class MirrorLocalApplyRepository(
    private val db: NocombroDatabase,
    private val entityApplyRepository: MirrorEntityApplyRepository,
    private val json: Json = Json { classDiscriminator = "_mirrorType" },
) {
    /**
     * Применяет рассчитанные remote changes одной Room-транзакцией.
     *
     * Пустой список является no-op. Метод ожидает уже выбранных planner-ом
     * победителей, но apply-репозитории дополнительно защищаются от stale-версий.
     */
    suspend fun apply(changes: List<MirrorPushEntityChange>) {
        if (changes.isEmpty()) return

        db.inTransaction {
            persistRemoteTombstones(changes)
            applyOrdered(changes.orderedForLocalApply())
        }
    }

    private suspend fun persistRemoteTombstones(changes: List<MirrorPushEntityChange>) {
        val tombstones = changes.mapNotNull { change ->
            val deletedAt = change.row.deletedAt ?: return@mapNotNull null
            MirrorDeletionJournalEntity(
                entityTable = change.table.tableName,
                syncId = change.row.syncId,
                rowJson = json.encodeToString<MirrorSyncRow>(change.row),
                deletedAt = deletedAt,
            )
        }
        if (tombstones.isNotEmpty()) {
            db.mirrorDeletionJournalDao.upsert(tombstones)
        }
    }

    private suspend fun applyOrdered(changes: List<MirrorPushEntityChange>) {
        changes.forEach { change ->
            if (change.table == MirrorSyncTable.BATCH_COST_PRICE) {
                applyBatchCostPrices(listOf(change.row as BatchCostPriceMirrorRow))
            } else {
                entityApplyRepository.applyChanges(listOf(change))
            }
        }
    }

    private suspend fun applyBatchCostPrices(rows: List<BatchCostPriceMirrorRow>) {
        if (rows.isEmpty()) return

        db.inTransaction {
            rows.forEach { row ->
                require(row.syncId == row.batchSyncId) {
                    "Batch cost syncId=${row.syncId} does not match batchSyncId=${row.batchSyncId}"
                }
                // Себестоимость идентифицируется sync_id партии, а локальный batchId
                // разрешается заново на каждой установке.
                val existing = db.batchCostDao.getBySyncId(row.syncId)
                if (existing != null && existing.versionAt() >= row.versionAt()) {
                    return@forEach
                }

                val batch = db.batchDao.getBatchBySyncId(row.batchSyncId)
                    ?: error("Missing batch dependency for syncId=${row.batchSyncId}")
                db.batchCostDao.upsert(
                    listOf(
                        BatchCostPriceEntity(
                            batchId = batch.id,
                            costPricePerUnit = row.costPricePerUnit,
                            batchSyncId = row.batchSyncId,
                            updatedAt = row.updatedAt,
                            deletedAt = row.deletedAt,
                        )
                    )
                )
            }
        }
    }
}

private fun BatchCostPriceEntity.versionAt() = deletedAt?.takeIf { it > updatedAt } ?: updatedAt

/**
 * Сортирует изменения в FK-безопасном порядке локального применения.
 *
 * Активные строки идут по возрастанию [MirrorSyncTable.applyOrder], tombstone -
 * по убыванию, чтобы сначала удалить дочерние строки.
 */
internal fun List<MirrorPushEntityChange>.orderedForLocalApply(): List<MirrorPushEntityChange> {
    val upserts = filter { it.row.deletedAt == null }
        .sortedBy { it.table.applyOrder }
    val deletes = filter { it.row.deletedAt != null }
        .sortedByDescending { it.table.applyOrder }
    return upserts + deletes
}
