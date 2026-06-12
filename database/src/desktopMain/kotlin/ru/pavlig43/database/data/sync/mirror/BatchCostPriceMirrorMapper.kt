package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchCostPriceEntity

/**
 * Преобразует себестоимость партии между локальной Room-моделью и mirror row.
 *
 * У себестоимости нет независимой межустановочной identity: ее `sync_id` равен
 * `batch_sync_id` родительской партии. Mapper проверяет этот инвариант в обоих
 * направлениях и заново разрешает локальный числовой `batchId` при pull.
 */
class BatchCostPriceMirrorMapper(
    private val db: NocombroDatabase,
) {
    /**
     * Строит mirror row и проверяет согласованность сохраненного `batchSyncId`.
     *
     * @throws IllegalArgumentException если entity ссылается на другую sync-партию.
     */
    suspend fun toMirrorRow(entity: BatchCostPriceEntity): BatchCostPriceMirrorRow {
        val batch = db.batchDao.getBatch(entity.batchId)
        require(entity.batchSyncId == batch.syncId) {
            "Batch cost syncId=${entity.batchSyncId} does not match batch syncId=${batch.syncId}"
        }
        return BatchCostPriceMirrorRow(
            syncId = entity.batchSyncId,
            batchSyncId = batch.syncId,
            costPricePerUnit = entity.costPricePerUnit,
            updatedAt = entity.updatedAt,
            deletedAt = entity.deletedAt,
        )
    }

    /**
     * Разрешает локальную партию по `batchSyncId` и создает Room entity.
     *
     * @throws IllegalArgumentException если identity строки и партии различаются.
     * @throws IllegalStateException если родительская партия еще не применена.
     */
    suspend fun fromMirrorRow(row: BatchCostPriceMirrorRow): BatchCostPriceEntity {
        require(row.syncId == row.batchSyncId) {
            "Batch cost syncId=${row.syncId} does not match batchSyncId=${row.batchSyncId}"
        }
        val batch = db.batchDao.getBatchBySyncId(row.batchSyncId)
            ?: error("Missing batch dependency for syncId=${row.batchSyncId}")
        return BatchCostPriceEntity(
            batchId = batch.id,
            costPricePerUnit = row.costPricePerUnit,
            batchSyncId = row.batchSyncId,
            updatedAt = row.updatedAt,
            deletedAt = row.deletedAt,
        )
    }
}
