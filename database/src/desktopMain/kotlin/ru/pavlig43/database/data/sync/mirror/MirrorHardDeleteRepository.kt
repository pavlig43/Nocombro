package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.NocombroDatabase

data class MirrorHardDeleteRequest(
    val table: MirrorSyncTable,
    val syncId: String,
    val tombstoneVersion: LocalDateTime,
)

data class MirrorHardDeleteResult(
    val deletedRows: Int,
    val stale: Boolean,
)

class MirrorHardDeleteRepository(
    private val db: NocombroDatabase,
    private val snapshotRepository: MirrorLocalSnapshotRepository = MirrorLocalSnapshotRepository(db),
) {
    @Suppress("CyclomaticComplexMethod", "ReturnCount", "UnreachableCode")
    suspend fun delete(request: MirrorHardDeleteRequest): MirrorHardDeleteResult {
        val existing = snapshotRepository.loadDatabaseSnapshot(listOf(request.table))
            .rowsByTable[request.table]
            .orEmpty()
            .firstOrNull { it.syncId == request.syncId }
            ?: return MirrorHardDeleteResult(0, stale = false)

        if (existing.versionAt() > request.tombstoneVersion) {
            return MirrorHardDeleteResult(0, stale = true)
        }

        val deletedRows = when (request.table) {
            MirrorSyncTable.VENDOR -> db.mirrorHardDeleteDao.deleteVendor(request.syncId)
            MirrorSyncTable.DOCUMENT -> db.mirrorHardDeleteDao.deleteDocument(request.syncId)
            MirrorSyncTable.DECLARATION -> db.mirrorHardDeleteDao.deleteDeclaration(request.syncId)
            MirrorSyncTable.PRODUCT -> db.mirrorHardDeleteDao.deleteProduct(request.syncId)
            MirrorSyncTable.TRANSACTION -> db.mirrorHardDeleteDao.deleteTransaction(request.syncId)
            MirrorSyncTable.EXPERIMENT -> db.mirrorHardDeleteDao.deleteExperiment(request.syncId)
            MirrorSyncTable.PRODUCT_SPECIFICATION ->
                db.mirrorHardDeleteDao.deleteProductSpecification(request.syncId)
            MirrorSyncTable.SAFETY_STOCK -> db.mirrorHardDeleteDao.deleteSafetyStock(request.syncId)
            MirrorSyncTable.EXPERIMENT_ENTRY -> db.mirrorHardDeleteDao.deleteExperimentEntry(request.syncId)
            MirrorSyncTable.EXPERIMENT_REMINDER ->
                db.mirrorHardDeleteDao.deleteExperimentReminder(request.syncId)
            MirrorSyncTable.PRODUCT_DECLARATION ->
                db.mirrorHardDeleteDao.deleteProductDeclaration(request.syncId)
            MirrorSyncTable.COMPOSITION -> db.mirrorHardDeleteDao.deleteComposition(request.syncId)
            MirrorSyncTable.BATCH -> db.mirrorHardDeleteDao.deleteBatch(request.syncId)
            MirrorSyncTable.BATCH_COST_PRICE -> db.mirrorHardDeleteDao.deleteBatchCostPrice(request.syncId)
            MirrorSyncTable.BATCH_MOVEMENT -> db.mirrorHardDeleteDao.deleteBatchMovement(request.syncId)
            MirrorSyncTable.REMINDER -> db.mirrorHardDeleteDao.deleteReminder(request.syncId)
            MirrorSyncTable.EXPENSE -> db.mirrorHardDeleteDao.deleteExpense(request.syncId)
            MirrorSyncTable.BUY -> db.mirrorHardDeleteDao.deleteBuy(request.syncId)
            MirrorSyncTable.SALE -> db.mirrorHardDeleteDao.deleteSale(request.syncId)
            MirrorSyncTable.FILE -> db.mirrorHardDeleteDao.deleteFile(request.syncId)
        }
        return MirrorHardDeleteResult(
            deletedRows = deletedRows,
            stale = false,
        )
    }
}
