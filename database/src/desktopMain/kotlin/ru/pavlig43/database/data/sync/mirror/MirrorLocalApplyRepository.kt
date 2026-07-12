package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
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

            changes.filter { it.row.deletedAt == null }
                .groupBy(MirrorPushEntityChange::table)
                .toSortedMap(compareBy(MirrorSyncTable::applyOrder))
                .forEach { (table, tableChanges) ->
                    applyActiveChanges(table, tableChanges)
                }

            val explicitKeys = explicitTombstones
                .mapTo(mutableSetOf()) { it.entityKey() }
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
                    .filter { it.entityKey() !in explicitKeys }
                    .map { MirrorPushEntityChange(it.table, it.row.markDeleted(deletedAt)) }
                persistedTombstones += persistNewestTombstones(generatedTombstones)

                (dependentRows + MirrorPushEntityChange(change.table, root))
                    .distinctBy { it.entityKey() }
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

    private suspend fun applyActiveChanges(
        table: MirrorSyncTable,
        changes: List<MirrorPushEntityChange>,
    ) {
        if (table == MirrorSyncTable.BATCH_COST_PRICE) {
            changes.forEach { applyBatchCostPrice(it.row as BatchCostPriceMirrorRow) }
        } else {
            entityApplyRepository.applyChanges(changes)
        }
    }

    private suspend fun persistNewestTombstones(changes: List<MirrorPushEntityChange>): Int {
        if (changes.isEmpty()) return 0
        val existing = db.mirrorDeletionJournalDao.getAll()
            .associateBy { MirrorEntityKey(requireNotNull(MirrorSyncTable.fromTableName(it.entityTable)), it.syncId) }
        val entries = changes.mapNotNull { change ->
            val deletedAt = change.row.deletedAt ?: return@mapNotNull null
            val key = change.entityKey()
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
        val collected = linkedMapOf<MirrorEntityKey, MirrorPushEntityChange>()
        val queue = ArrayDeque<MirrorEntityKey>()
        queue.add(MirrorEntityKey(rootTable, rootSyncId))
        while (queue.isNotEmpty()) {
            val parent = queue.removeFirst()
            snapshot.rowsByTable.forEach { (table, rows) ->
                rows.filter { it.dependsOn(parent) }.forEach { row ->
                    val key = MirrorEntityKey(table, row.syncId)
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

private data class TransactionResult(
    val persistedTombstones: Int,
    val deletedRows: Int,
)

private fun BatchCostPriceEntity.versionAt(): LocalDateTime =
    deletedAt?.takeIf { it > updatedAt } ?: updatedAt

internal fun List<MirrorPushEntityChange>.orderedForLocalApply(): List<MirrorPushEntityChange> {
    val upserts = filter { it.row.deletedAt == null }.sortedBy { it.table.applyOrder }
    val deletes = filter { it.row.deletedAt != null }.sortedByDescending { it.table.applyOrder }
    return upserts + deletes
}
