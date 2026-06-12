package ru.pavlig43.database

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.assertions.throwables.shouldThrow
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPullRequest
import ru.pavlig43.database.data.sync.mirror.MirrorPullResult
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorPushResult
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlanner
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationService
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteStatus
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRemoteGateway
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.VendorMirrorRow
import ru.pavlig43.database.data.sync.mirror.BatchCostPriceMirrorRow
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.createManagedTestDatabase
import ru.pavlig43.testkit.database.createSeededManagedTestDatabase

class MirrorLocalApplyRepositoryTest : DesktopMainDispatcherFunSpec({

    test("full mirror snapshot restores every business table into an empty database") {
        val source = createSeededManagedTestDatabase()
        val target = createManagedTestDatabase()
        try {
            val sourceSnapshot = MirrorLocalSnapshotRepository(source.database)
                .loadSnapshot(MirrorSyncTable.mirroredBusinessTables)
            val changes = MirrorSyncTable.mirroredBusinessTables.flatMap { table ->
                sourceSnapshot.rowsByTable[table].orEmpty().map { row ->
                    MirrorPushEntityChange(table = table, row = row)
                }
            }

            MirrorLocalApplyRepository(
                db = target.database,
                entityApplyRepository = MirrorEntityApplyRepository(target.database),
            ).apply(changes)

            val restoredSnapshot = MirrorLocalSnapshotRepository(target.database)
                .loadSnapshot(MirrorSyncTable.mirroredBusinessTables)

            MirrorSyncTable.mirroredBusinessTables.forEach { table ->
                restoredSnapshot.rowsByTable[table].orEmpty()
                    .map { it.syncId }
                    .sorted() shouldContainExactly sourceSnapshot.rowsByTable[table].orEmpty()
                    .map { it.syncId }
                    .sorted()
            }

            restoredSnapshot.rowsByTable.keys shouldBe sourceSnapshot.rowsByTable.keys
            target.database.batchCostDao.getAll().map { it.costPricePerUnit }.sorted() shouldContainExactly
                source.database.batchCostDao.getAll().map { it.costPricePerUnit }.sorted()
        } finally {
            target.close()
            source.close()
        }
    }

    test("rebuild pushes the full local snapshot and tombstones remote only rows") {
        val local = createSeededManagedTestDatabase()
        try {
            val remoteOnly = VendorMirrorRow(
                syncId = "remote-only-vendor",
                displayName = "Remote only",
                comment = "",
                updatedAt = LocalDateTime(2026, 6, 1, 10, 0),
            )
            val gateway = CapturingMirrorGateway(
                remoteSnapshot = MirrorRemoteSnapshot(
                    loadedAt = LocalDateTime(2026, 6, 10, 10, 0),
                    rowsByTable = mapOf(MirrorSyncTable.VENDOR to listOf(remoteOnly)),
                )
            )
            val applyRepository = MirrorLocalApplyRepository(
                db = local.database,
                entityApplyRepository = MirrorEntityApplyRepository(local.database),
            )
            val service = MirrorReconciliationService(
                localSnapshotRepository = MirrorLocalSnapshotRepository(local.database),
                remoteGateway = gateway,
                planner = MirrorReconciliationPlanner(),
                localApplyRepository = applyRepository,
            )

            val result = service.rebuildRemoteFromLocal().getOrThrow()
            val tombstone = gateway.pushedChanges.single {
                it.row.syncId == remoteOnly.syncId
            }.row

            result.pushedRows shouldBe gateway.pushedChanges.size - 1
            result.tombstonedRows shouldBe 1
            tombstone.deletedAt.shouldNotBeNull()
        } finally {
            local.close()
        }
    }

    test("batch cost is restored from mirror with the batch sync id") {
        val target = createSeededManagedTestDatabase()
        try {
            val batch = target.database.batchDao.getBatch(1)
            val row = BatchCostPriceMirrorRow(
                syncId = batch.syncId,
                batchSyncId = batch.syncId,
                costPricePerUnit = 98_765,
                updatedAt = LocalDateTime(2099, 6, 11, 12, 0),
            )

            MirrorLocalApplyRepository(
                db = target.database,
                entityApplyRepository = MirrorEntityApplyRepository(target.database),
            ).apply(listOf(MirrorPushEntityChange(MirrorSyncTable.BATCH_COST_PRICE, row)))

            val restored = target.database.batchCostDao.getBySyncId(batch.syncId)
                .shouldNotBeNull()
            restored.batchId shouldBe batch.id
            restored.batchSyncId shouldBe batch.syncId
            restored.costPricePerUnit shouldBe 98_765
        } finally {
            target.close()
        }
    }

    test("batch cost mirror row with mismatched ids is rejected") {
        val target = createSeededManagedTestDatabase()
        try {
            val batch = target.database.batchDao.getBatch(1)
            val row = BatchCostPriceMirrorRow(
                syncId = "wrong-cost-id",
                batchSyncId = batch.syncId,
                costPricePerUnit = 98_765,
                updatedAt = LocalDateTime(2026, 6, 11, 12, 0),
            )

            shouldThrow<IllegalArgumentException> {
                MirrorLocalApplyRepository(
                    db = target.database,
                    entityApplyRepository = MirrorEntityApplyRepository(target.database),
                ).apply(listOf(MirrorPushEntityChange(MirrorSyncTable.BATCH_COST_PRICE, row)))
            }
        } finally {
            target.close()
        }
    }
})

private class CapturingMirrorGateway(
    private val remoteSnapshot: MirrorRemoteSnapshot,
) : MirrorSyncRemoteGateway {
    var pushedChanges: List<MirrorPushEntityChange> = emptyList()

    override suspend fun getStatus() = MirrorRemoteStatus(
        configured = true,
        availableTables = MirrorSyncTable.entries.mapTo(mutableSetOf()) { it.tableName },
        checkedAt = remoteSnapshot.loadedAt,
    )

    override suspend fun loadRemoteSnapshot(tables: List<MirrorSyncTable>) =
        Result.success(remoteSnapshot)

    override suspend fun pushMirrorState(changes: List<MirrorPushEntityChange>): Result<MirrorPushResult> {
        pushedChanges = changes
        return Result.success(
            MirrorPushResult(
                pushedAt = remoteSnapshot.loadedAt,
                affectedTables = changes.mapTo(mutableSetOf(), MirrorPushEntityChange::table),
            )
        )
    }

    override suspend fun pullMirrorState(request: MirrorPullRequest) =
        Result.success(MirrorPullResult(remoteSnapshot.loadedAt, emptyList()))
}
