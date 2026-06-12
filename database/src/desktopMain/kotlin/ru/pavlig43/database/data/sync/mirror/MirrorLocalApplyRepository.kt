package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.inTransaction

data class MirrorLocalApplyResult(
    val persistedTombstones: Int,
    val deletedRows: Int,
)

class MirrorLocalApplyRepository(
    private val db: NocombroDatabase,
    private val entityApplyRepository: MirrorEntityApplyRepository,
    private val hardDeleteRepository: MirrorHardDeleteRepository = MirrorHardDeleteRepository(db),
    private val json: Json = Json { classDiscriminator = "_mirrorType" },
) {
    private val snapshotRepository = MirrorLocalSnapshotRepository(db)

    suspend fun apply(changes: List<MirrorPushEntityChange>): MirrorLocalApplyResult {
        if (changes.isEmpty()) return MirrorLocalApplyResult(0, 0)

        val transactionResult = db.inTransaction {
            val explicitTombstones = changes.filter { it.row.deletedAt != null }
            var persistedTombstones = persistNewestTombstones(explicitTombstones)

            for (change in changes.filter { it.row.deletedAt == null }.sortedBy { it.table.applyOrder }) {
                applyActiveChange(change)
            }

            val explicitKeys = explicitTombstones
                .mapTo(mutableSetOf()) { ApplyEntityKey(it.table, it.row.syncId) }
            var deletedRows = 0

            explicitTombstones.sortedByDescending { it.table.applyOrder }.forEach { change ->
                val deletedAt = requireNotNull(change.row.deletedAt)
                val snapshot = snapshotRepository.loadDatabaseSnapshot(MirrorSyncTable.mirroredBusinessTables)
                val root = snapshot.rowsByTable[change.table].orEmpty()
                    .firstOrNull { it.syncId == change.row.syncId }
                    ?: return@forEach
                if (root.versionAt() > change.row.versionAt()) return@forEach

                val dependentRows = collectDependentRows(snapshot, change.table, change.row.syncId)
                    .sortedByDescending { it.table.applyOrder }
                val generatedTombstones = dependentRows
                    .filter { ApplyEntityKey(it.table, it.row.syncId) !in explicitKeys }
                    .map { MirrorPushEntityChange(it.table, it.row.markDeleted(deletedAt)) }
                persistedTombstones += persistNewestTombstones(generatedTombstones)

                (dependentRows + MirrorPushEntityChange(change.table, root))
                    .distinctBy { ApplyEntityKey(it.table, it.row.syncId) }
                    .sortedByDescending { it.table.applyOrder }
                    .forEach { deletion ->
                        val result = hardDeleteRepository.delete(
                            MirrorHardDeleteRequest(
                                table = deletion.table,
                                syncId = deletion.row.syncId,
                                tombstoneVersion = maxOf(deletedAt, deletion.row.versionAt()),
                            )
                        )
                        deletedRows += result.deletedRows
                    }
            }
            TransactionResult(persistedTombstones, deletedRows)
        }

        return MirrorLocalApplyResult(
            persistedTombstones = transactionResult.persistedTombstones,
            deletedRows = transactionResult.deletedRows,
        )
    }

    private suspend fun applyActiveChange(change: MirrorPushEntityChange) {
        if (change.table == MirrorSyncTable.BATCH_COST_PRICE) {
            applyBatchCostPrice(change.row as BatchCostPriceMirrorRow)
        } else {
            entityApplyRepository.applyChanges(listOf(change))
        }
    }

    private suspend fun persistNewestTombstones(changes: List<MirrorPushEntityChange>): Int {
        if (changes.isEmpty()) return 0
        val existing = db.mirrorDeletionJournalDao.getAll()
            .associateBy { ApplyEntityKey(requireNotNull(MirrorSyncTable.fromTableName(it.entityTable)), it.syncId) }
        val entries = changes.mapNotNull { change ->
            val deletedAt = change.row.deletedAt ?: return@mapNotNull null
            val key = ApplyEntityKey(change.table, change.row.syncId)
            if (existing[key]?.deletedAt?.let { it >= deletedAt } == true) return@mapNotNull null
            MirrorDeletionJournalEntity(
                entityTable = change.table.tableName,
                syncId = change.row.syncId,
                rowJson = json.encodeToString<MirrorSyncRow>(change.row),
                deletedAt = deletedAt,
            )
        }
        if (entries.isNotEmpty()) db.mirrorDeletionJournalDao.upsert(entries)
        return entries.size
    }

    private suspend fun applyBatchCostPrice(row: BatchCostPriceMirrorRow) {
        require(row.syncId == row.batchSyncId) {
            "Batch cost syncId=${row.syncId} does not match batchSyncId=${row.batchSyncId}"
        }
        val existing = db.batchCostDao.getBySyncId(row.syncId)
        if (existing != null && existing.versionAt() >= row.versionAt()) return
        val batch = db.batchDao.getBatchBySyncId(row.batchSyncId)
            ?: error("Missing batch dependency for syncId=${row.batchSyncId}")
        db.batchCostDao.upsert(
            listOf(
                BatchCostPriceEntity(
                    batchId = batch.id,
                    costPricePerUnit = row.costPricePerUnit,
                    batchSyncId = row.batchSyncId,
                    updatedAt = row.updatedAt,
                    deletedAt = null,
                )
            )
        )
    }

    private fun collectDependentRows(
        snapshot: MirrorLocalSnapshot,
        rootTable: MirrorSyncTable,
        rootSyncId: String,
    ): List<MirrorPushEntityChange> {
        val collected = linkedMapOf<ApplyEntityKey, MirrorPushEntityChange>()
        val queue = ArrayDeque<ApplyEntityKey>()
        queue.add(ApplyEntityKey(rootTable, rootSyncId))
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            snapshot.rowsByTable.forEach { (table, rows) ->
                rows.filter { it.dependsOn(parent) }.forEach { row ->
                    val key = ApplyEntityKey(table, row.syncId)
                    if (key !in collected) {
                        collected[key] = MirrorPushEntityChange(table, row)
                        queue.add(key)
                    }
                }
            }
        }
        return collected.values.toList()
    }
}

private data class ApplyEntityKey(val table: MirrorSyncTable, val syncId: String)
private data class TransactionResult(
    val persistedTombstones: Int,
    val deletedRows: Int,
)

private fun MirrorSyncRow.dependsOn(parent: ApplyEntityKey): Boolean = when (this) {
    is DeclarationMirrorRow -> parent.matches(MirrorSyncTable.VENDOR, vendorSyncId)
    is ProductSpecificationMirrorRow -> parent.matches(MirrorSyncTable.PRODUCT, productSyncId)
    is SafetyStockMirrorRow -> parent.matches(MirrorSyncTable.PRODUCT, productSyncId)
    is ExperimentEntryMirrorRow -> parent.matches(MirrorSyncTable.EXPERIMENT, experimentSyncId)
    is ExperimentReminderMirrorRow -> parent.matches(MirrorSyncTable.EXPERIMENT, experimentSyncId)
    is ProductDeclarationMirrorRow ->
        parent.matches(MirrorSyncTable.PRODUCT, productSyncId) ||
            parent.matches(MirrorSyncTable.DECLARATION, declarationSyncId)
    is CompositionMirrorRow ->
        parent.matches(MirrorSyncTable.PRODUCT, parentSyncId) ||
            parent.matches(MirrorSyncTable.PRODUCT, productSyncId)
    is BatchMirrorRow ->
        parent.matches(MirrorSyncTable.PRODUCT, productSyncId) ||
            parent.matches(MirrorSyncTable.DECLARATION, declarationSyncId)
    is BatchCostPriceMirrorRow -> parent.matches(MirrorSyncTable.BATCH, batchSyncId)
    is BatchMovementMirrorRow ->
        parent.matches(MirrorSyncTable.BATCH, batchSyncId) ||
            parent.matches(MirrorSyncTable.TRANSACTION, transactionSyncId)
    is ReminderMirrorRow -> parent.matches(MirrorSyncTable.TRANSACTION, transactionSyncId)
    is ExpenseMirrorRow -> transactionSyncId?.let { parent.matches(MirrorSyncTable.TRANSACTION, it) } == true
    is BuyMirrorRow ->
        parent.matches(MirrorSyncTable.TRANSACTION, transactionSyncId) ||
            parent.matches(MirrorSyncTable.BATCH_MOVEMENT, movementSyncId)
    is SaleMirrorRow ->
        parent.matches(MirrorSyncTable.TRANSACTION, transactionSyncId) ||
            parent.matches(MirrorSyncTable.BATCH_MOVEMENT, movementSyncId) ||
            parent.matches(MirrorSyncTable.VENDOR, clientSyncId)
    is FileMirrorRow -> parent.matches(ownerType.mirrorTable(), ownerSyncId)
    is VendorMirrorRow,
    is DocumentMirrorRow,
    is ProductMirrorRow,
    is TransactionMirrorRow,
    is ExperimentMirrorRow,
    -> false
}

private fun ApplyEntityKey.matches(table: MirrorSyncTable, syncId: String) =
    this.table == table && this.syncId == syncId

private fun OwnerType.mirrorTable(): MirrorSyncTable = when (this) {
    OwnerType.DECLARATION -> MirrorSyncTable.DECLARATION
    OwnerType.PRODUCT -> MirrorSyncTable.PRODUCT
    OwnerType.VENDOR -> MirrorSyncTable.VENDOR
    OwnerType.DOCUMENT -> MirrorSyncTable.DOCUMENT
    OwnerType.TRANSACTION -> MirrorSyncTable.TRANSACTION
    OwnerType.EXPENSE -> MirrorSyncTable.EXPENSE
    OwnerType.EXPERIMENT_ENTRY -> MirrorSyncTable.EXPERIMENT_ENTRY
}

private fun BatchCostPriceEntity.versionAt(): LocalDateTime =
    deletedAt?.takeIf { it > updatedAt } ?: updatedAt

internal fun List<MirrorPushEntityChange>.orderedForLocalApply(): List<MirrorPushEntityChange> {
    val upserts = filter { it.row.deletedAt == null }.sortedBy { it.table.applyOrder }
    val deletes = filter { it.row.deletedAt != null }.sortedByDescending { it.table.applyOrder }
    return upserts + deletes
}
