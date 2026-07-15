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
        assertEquals(0, plan.conflicts.size)
    }

    /** Проверяет, что равная версия с другим содержимым попадает в конфликты. */
    @Test
    fun equalVersionWithDifferentPayloadIsConflict() {
        val localRow = row(updatedAt = "2026-01-01T00:00:00")
        val remoteRow = localRow.copy(title = "Remote title")

        val plan = planner.plan(snapshot(localRow), snapshot(remoteRow))

        assertEquals(0, plan.pushChanges.size)
        assertEquals(0, plan.pullChanges.size)
        assertEquals(1, plan.conflicts.size)
    }

    /** Проверяет, что разные абсолютные пути одного файла не создают конфликт. */
    @Test
    fun deviceLocalFilePathDoesNotCreateConflict() {
        val version = LocalDateTime.parse("2026-01-01T00:00:00")
        val local = MobileFileMirrorRow(
            syncId = "file-sync-id",
            ownerType = MobileFileOwnerType.EXPERIMENT_ENTRY,
            ownerSyncId = "entry-sync-id",
            displayName = "report.pdf",
            path = "/data/user/0/local/files/report.pdf",
            remoteObjectKey = "files/experiment_entry/file-sync-id/report.pdf",
            remoteStorageProvider = "S3",
            updatedAt = version,
            deletedAt = null,
        )
        val remote = local.copy(path = "C:/Users/remote/AppData/Roaming/Nocombro/files/report.pdf")

        val plan = planner.plan(fileSnapshot(local), fileSnapshot(remote))

        assertEquals(0, plan.pushChanges.size)
        assertEquals(0, plan.pullChanges.size)
        assertEquals(0, plan.conflicts.size)
    }

    /** Проверяет подсказку о разрешении конфликта через настольный Doctor. */
    @Test
    fun conflictStatusDirectsUserToDesktopDoctor() {
        val localRow = row(updatedAt = "2026-01-01T00:00:00")
        val remoteRow = localRow.copy(title = "Remote title")
        val conflict = MobileVersionConflict(MobileMirrorTable.EXPERIMENT, localRow, remoteRow)

        val text = MobileSyncStatus(
            configured = true,
            checkedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            conflicts = listOf(conflict),
        ).toStatusText()

        assertEquals(MOBILE_SYNC_CONFLICT_HINT, text)
    }

    private fun snapshot(row: MobileMirrorRow): MobileMirrorSnapshot {
        return MobileMirrorSnapshot(
            loadedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            rowsByTable = mapOf(MobileMirrorTable.EXPERIMENT to listOf(row)),
        )
    }

    /** Создаёт минимальный снимок таблицы файлов для проверки планировщика. */
    private fun fileSnapshot(row: MobileFileMirrorRow): MobileMirrorSnapshot {
        return MobileMirrorSnapshot(
            loadedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            rowsByTable = mapOf(MobileMirrorTable.FILE to listOf(row)),
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
