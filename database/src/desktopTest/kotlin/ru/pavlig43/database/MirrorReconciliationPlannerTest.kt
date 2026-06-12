package ru.pavlig43.database

import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.sync.mirror.BatchCostPriceMirrorRow
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPlanner
import ru.pavlig43.database.data.sync.mirror.MirrorRemoteSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRow
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.orderedForLocalApply
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

class MirrorReconciliationPlannerTest : DesktopMainDispatcherFunSpec({

    test("planner chooses newest side and ignores equal versions") {
        val older = LocalDateTime(2026, 6, 9, 10, 0)
        val newer = LocalDateTime(2026, 6, 9, 11, 0)
        val planner = MirrorReconciliationPlanner()

        val plan = planner.plan(
            localSnapshot = plannerLocalSnapshot(
                plannerRow("only-local", older),
                plannerRow("local-newer", newer),
                plannerRow("remote-newer", older),
                plannerRow("equal", newer),
            ),
            remoteSnapshot = plannerRemoteSnapshot(
                plannerRow("only-remote", older),
                plannerRow("local-newer", older),
                plannerRow("remote-newer", newer),
                plannerRow("equal", newer),
            ),
        )

        plan.pushChanges.map { it.row.syncId } shouldContainExactly listOf(
            "local-newer",
            "only-local",
        )
        plan.pullChanges.map { it.row.syncId } shouldContainExactly listOf(
            "only-remote",
            "remote-newer",
        )
    }

    test("newer deletion wins but remote tombstone without local row needs no apply") {
        val updatedAt = LocalDateTime(2026, 6, 9, 10, 0)
        val deletedAt = LocalDateTime(2026, 6, 9, 12, 0)
        val planner = MirrorReconciliationPlanner()

        val plan = planner.plan(
            localSnapshot = plannerLocalSnapshot(
                plannerRow("delete-local", updatedAt),
                plannerRow("remote-tombstone-over-local", updatedAt),
            ),
            remoteSnapshot = plannerRemoteSnapshot(
                plannerRow("remote-tombstone-only", updatedAt, deletedAt),
                plannerRow("delete-local", updatedAt, deletedAt),
                plannerRow("remote-tombstone-over-local", updatedAt, deletedAt),
            ),
        )

        plan.pushChanges shouldContainExactly emptyList()
        plan.pullChanges.map { it.row.syncId } shouldContainExactly listOf(
            "delete-local",
            "remote-tombstone-over-local",
        )
    }

    test("local newer tombstone is pushed over a live remote row") {
        val older = LocalDateTime(2026, 6, 9, 10, 0)
        val deletedAt = LocalDateTime(2026, 6, 9, 12, 0)

        val plan = MirrorReconciliationPlanner().plan(
            localSnapshot = plannerLocalSnapshot(
                plannerRow("local-delete", older, deletedAt),
            ),
            remoteSnapshot = plannerRemoteSnapshot(
                plannerRow("local-delete", older),
            ),
        )

        plan.pushChanges.map { it.row.syncId } shouldContainExactly listOf("local-delete")
        plan.pullChanges shouldContainExactly emptyList()
    }

    test("local apply orders parent upserts first and child deletes first") {
        val liveAt = LocalDateTime(2026, 6, 9, 10, 0)
        val deletedAt = LocalDateTime(2026, 6, 9, 12, 0)
        val changes = listOf(
            ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange(
                MirrorSyncTable.VENDOR,
                ru.pavlig43.database.data.sync.mirror.VendorMirrorRow(
                    syncId = "vendor-delete",
                    displayName = "Vendor",
                    comment = "",
                    updatedAt = liveAt,
                    deletedAt = deletedAt,
                ),
            ),
            ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange(
                MirrorSyncTable.DECLARATION,
                ru.pavlig43.database.data.sync.mirror.DeclarationMirrorRow(
                    syncId = "declaration-delete",
                    displayName = "Declaration",
                    createdAt = LocalDate(2026, 6, 9),
                    vendorSyncId = "vendor-delete",
                    vendorName = "Vendor",
                    bornDate = LocalDate(2026, 6, 1),
                    bestBefore = LocalDate(2026, 12, 1),
                    observeFromNotification = false,
                    updatedAt = liveAt,
                    deletedAt = deletedAt,
                ),
            ),
            ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange(
                MirrorSyncTable.VENDOR,
                ru.pavlig43.database.data.sync.mirror.VendorMirrorRow(
                    syncId = "vendor-upsert",
                    displayName = "Vendor",
                    comment = "",
                    updatedAt = liveAt,
                ),
            ),
            ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange(
                MirrorSyncTable.DECLARATION,
                ru.pavlig43.database.data.sync.mirror.DeclarationMirrorRow(
                    syncId = "declaration-upsert",
                    displayName = "Declaration",
                    createdAt = LocalDate(2026, 6, 9),
                    vendorSyncId = "vendor-upsert",
                    vendorName = "Vendor",
                    bornDate = LocalDate(2026, 6, 1),
                    bestBefore = LocalDate(2026, 12, 1),
                    observeFromNotification = false,
                    updatedAt = liveAt,
                ),
            ),
        )

        changes.orderedForLocalApply().map { it.row.syncId } shouldContainExactly listOf(
            "vendor-upsert",
            "declaration-upsert",
            "declaration-delete",
            "vendor-delete",
        )
    }
})

private fun plannerRow(
    syncId: String,
    updatedAt: LocalDateTime,
    deletedAt: LocalDateTime? = null,
) = BatchCostPriceMirrorRow(
    syncId = syncId,
    batchSyncId = "batch-$syncId",
    costPricePerUnit = 100,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

private fun plannerLocalSnapshot(
    vararg rows: MirrorSyncRow,
) = MirrorLocalSnapshot(
    loadedAt = LocalDateTime(2026, 6, 9, 12, 0),
    rowsByTable = mapOf(MirrorSyncTable.BATCH_COST_PRICE to rows.toList()),
)

private fun plannerRemoteSnapshot(
    vararg rows: MirrorSyncRow,
) = MirrorRemoteSnapshot(
    loadedAt = LocalDateTime(2026, 6, 9, 12, 0),
    rowsByTable = mapOf(MirrorSyncTable.BATCH_COST_PRICE to rows.toList()),
)
