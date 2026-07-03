package ru.pavlig43.nocombro.mobile.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDateTime

class MobileReconciliationPlannerTest {
    private val planner = MobileReconciliationPlanner()

    @Test
    fun localWins() {
        val local = snapshot(row(updatedAt = "2026-01-02T00:00:00"))
        val remote = snapshot(row(updatedAt = "2026-01-01T00:00:00"))

        val plan = planner.plan(local, remote)

        assertEquals(1, plan.pushChanges.size)
        assertEquals(0, plan.pullChanges.size)
    }

    @Test
    fun remoteWins() {
        val local = snapshot(row(updatedAt = "2026-01-01T00:00:00"))
        val remote = snapshot(row(updatedAt = "2026-01-02T00:00:00"))

        val plan = planner.plan(local, remote)

        assertEquals(0, plan.pushChanges.size)
        assertEquals(1, plan.pullChanges.size)
    }

    @Test
    fun tombstoneWins() {
        val local = snapshot(row(updatedAt = "2026-01-03T00:00:00", deletedAt = "2026-01-03T00:00:00"))
        val remote = snapshot(row(updatedAt = "2026-01-02T00:00:00"))

        val plan = planner.plan(local, remote)

        assertEquals(1, plan.pushChanges.size)
        assertEquals(0, plan.pullChanges.size)
    }

    @Test
    fun equalVersionNoOp() {
        val local = snapshot(row(updatedAt = "2026-01-01T00:00:00"))
        val remote = snapshot(row(updatedAt = "2026-01-01T00:00:00"))

        val plan = planner.plan(local, remote)

        assertEquals(0, plan.pushChanges.size)
        assertEquals(0, plan.pullChanges.size)
    }

    private fun snapshot(row: MobileMirrorRow): MobileMirrorSnapshot {
        return MobileMirrorSnapshot(
            loadedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            rowsByTable = mapOf(MobileMirrorTable.EXPERIMENT to listOf(row)),
        )
    }

    private fun row(
        updatedAt: String,
        deletedAt: String? = null,
    ): MobileExperimentMirrorRow {
        return MobileExperimentMirrorRow(
            syncId = "experiment-sync-id",
            title = "Title",
            ideaDescription = "Idea",
            isArchived = false,
            updatedAt = LocalDateTime.parse(updatedAt),
            deletedAt = deletedAt?.let(LocalDateTime::parse),
        )
    }
}
