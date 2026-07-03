package ru.pavlig43.nocombro.mobile.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

class MobileSyncPreviewBuilderTest {
    private val planner = MobileReconciliationPlanner()

    @Test
    fun localWinsBuildsPushGroupWithRemoteBeforeLocalAfter() {
        val local = snapshot(
            experiment(title = "Local", updatedAt = "2026-01-02T00:00:00"),
        )
        val remote = snapshot(
            experiment(title = "Remote", updatedAt = "2026-01-01T00:00:00"),
        )

        val groups = pushGroups(local, remote)

        assertEquals(1, groups.size)
        assertEquals("Local", groups.single().title)
        assertEquals("Remote", groups.single().metadata?.diffs?.single { it.label == "Название" }?.before)
        assertEquals("Local", groups.single().metadata?.diffs?.single { it.label == "Название" }?.after)
    }

    @Test
    fun remoteWinsBuildsPullGroupWithLocalBeforeRemoteAfter() {
        val local = snapshot(
            experiment(title = "Local", updatedAt = "2026-01-01T00:00:00"),
        )
        val remote = snapshot(
            experiment(title = "Remote", updatedAt = "2026-01-02T00:00:00"),
        )

        val groups = pullGroups(local, remote)

        assertEquals(1, groups.size)
        assertEquals("Remote", groups.single().title)
        assertEquals("Local", groups.single().metadata?.diffs?.single { it.label == "Название" }?.before)
        assertEquals("Remote", groups.single().metadata?.diffs?.single { it.label == "Название" }?.after)
    }

    @Test
    fun tombstoneWinsBuildsDeletedMetadata() {
        val local = snapshot(
            experiment(
                title = "Deleted",
                updatedAt = "2026-01-03T00:00:00",
                deletedAt = "2026-01-03T00:00:00",
            ),
        )
        val remote = snapshot(
            experiment(title = "Remote", updatedAt = "2026-01-02T00:00:00"),
        )

        val groups = pushGroups(local, remote)

        assertEquals(1, groups.size)
        assertTrue(groups.single().metadata?.deleted == true)
        assertEquals("Удаление", groups.single().metadata?.actionLabel)
    }

    @Test
    fun equalVersionNoOpBuildsNoGroups() {
        val local = snapshot(
            experiment(updatedAt = "2026-01-01T00:00:00"),
        )
        val remote = snapshot(
            experiment(updatedAt = "2026-01-01T00:00:00"),
        )

        assertTrue(pushGroups(local, remote).isEmpty())
        assertTrue(pullGroups(local, remote).isEmpty())
    }

    @Test
    fun fileOnlyChangeIsNestedUnderEntry() {
        val local = snapshot(
            experiment(),
            entry(),
            file(displayName = "local.txt", updatedAt = "2026-01-02T00:00:00"),
        )
        val remote = snapshot(
            experiment(),
            entry(),
            file(displayName = "remote.txt", updatedAt = "2026-01-01T00:00:00"),
        )

        val group = pushGroups(local, remote).single()

        assertEquals("Experiment", group.title)
        assertEquals(1, group.entries.size)
        assertEquals("Запись от 01.01.2026", group.entries.single().title)
        assertEquals("local.txt", group.entries.single().files.single().title)
        assertEquals("remote.txt", group.entries.single().files.single().diffs.single { it.label == "Имя" }.before)
        assertEquals("local.txt", group.entries.single().files.single().diffs.single { it.label == "Имя" }.after)
    }

    @Test
    fun fileLinkedByLocalIdDoesNotEnterPreview() {
        val local = snapshot(
            experiment(),
            entry(syncId = "entry-sync"),
            file(ownerSyncId = "1", updatedAt = "2026-01-02T00:00:00"),
        )
        val remote = snapshot(
            experiment(),
            entry(syncId = "entry-sync"),
            file(ownerSyncId = "1", updatedAt = "2026-01-01T00:00:00"),
        )

        val groups = pushGroups(local, remote)

        assertTrue(groups.isEmpty())
    }

    private fun pushGroups(
        local: MobileMirrorSnapshot,
        remote: MobileMirrorSnapshot,
    ): List<MobileExperimentChangeGroup> {
        val plan = planner.plan(local, remote)
        return buildExperimentChangeGroups(plan.pushChanges, before = remote, after = local)
    }

    private fun pullGroups(
        local: MobileMirrorSnapshot,
        remote: MobileMirrorSnapshot,
    ): List<MobileExperimentChangeGroup> {
        val plan = planner.plan(local, remote)
        return buildExperimentChangeGroups(plan.pullChanges, before = local, after = remote)
    }

    private fun snapshot(vararg rows: MobileMirrorRow): MobileMirrorSnapshot {
        return MobileMirrorSnapshot(
            loadedAt = LocalDateTime.parse("2026-01-01T00:00:00"),
            rowsByTable = rows.groupBy { row ->
                when (row) {
                    is MobileExperimentMirrorRow -> MobileMirrorTable.EXPERIMENT
                    is MobileExperimentEntryMirrorRow -> MobileMirrorTable.EXPERIMENT_ENTRY
                    is MobileExperimentReminderMirrorRow -> MobileMirrorTable.EXPERIMENT_REMINDER
                    is MobileFileMirrorRow -> MobileMirrorTable.FILE
                }
            },
        )
    }

    private fun experiment(
        syncId: String = "experiment-sync",
        title: String = "Experiment",
        updatedAt: String = "2026-01-01T00:00:00",
        deletedAt: String? = null,
    ) = MobileExperimentMirrorRow(
        syncId = syncId,
        title = title,
        ideaDescription = "Idea",
        isArchived = false,
        updatedAt = LocalDateTime.parse(updatedAt),
        deletedAt = deletedAt?.let(LocalDateTime::parse),
    )

    private fun entry(
        syncId: String = "entry-sync",
        experimentSyncId: String = "experiment-sync",
        updatedAt: String = "2026-01-01T00:00:00",
    ) = MobileExperimentEntryMirrorRow(
        syncId = syncId,
        experimentSyncId = experimentSyncId,
        entryDate = LocalDate.parse("2026-01-01"),
        createdAt = LocalDateTime.parse("2026-01-01T10:00:00"),
        content = "Entry content",
        updatedAt = LocalDateTime.parse(updatedAt),
        deletedAt = null,
    )

    private fun file(
        syncId: String = "file-sync",
        ownerSyncId: String = "entry-sync",
        displayName: String = "file.txt",
        updatedAt: String = "2026-01-01T00:00:00",
    ) = MobileFileMirrorRow(
        syncId = syncId,
        ownerType = MobileFileOwnerType.EXPERIMENT_ENTRY,
        ownerSyncId = ownerSyncId,
        displayName = displayName,
        path = "/tmp/$displayName",
        remoteObjectKey = "entries/$displayName",
        remoteStorageProvider = "S3",
        updatedAt = LocalDateTime.parse(updatedAt),
        deletedAt = null,
    )
}
