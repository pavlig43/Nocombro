package ru.pavlig43.nocombro.mobile.sync

import java.nio.file.Files
import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MobileSyncReportStoreTest {
    @Test
    fun `report contains both directions fields deletions and nested files`() {
        val store = MobileSyncReportStore(temporaryFilesDir())

        val report = store.write(fullPreview()).getOrThrow().readText()

        assertContains(report, "## К отправке")
        assertContains(report, "## К получению")
        assertContains(report, "#### Опыт")
        assertContains(report, "#### Напоминания")
        assertContains(report, "#### Записи")
        assertContains(report, "Файлы:")
        assertContains(report, "Удаление")
        assertContains(report, "Название: `Старое` → `Новое`")
        assertContains(report, "**protocol.pdf** — Создание")
        assertContains(report, "Не обновляется при открытии.")
    }

    @Test
    fun `next check replaces one file and leaves no temporary files`() {
        val filesDir = temporaryFilesDir()
        val store = MobileSyncReportStore(filesDir)
        val first = fullPreview(snapshotAt = "2026-08-02T10:00:00")
        val second = fullPreview(snapshotAt = "2026-08-02T11:00:00")

        val firstFile = store.write(first).getOrThrow()
        val secondFile = store.write(second).getOrThrow()

        assertEquals(firstFile.absolutePath, secondFile.absolutePath)
        assertEquals(MOBILE_SYNC_REPORT_FILE_NAME, secondFile.name)
        assertEquals(second.snapshotAt, store.latestSnapshotAt())
        assertEquals(
            listOf(MOBILE_SYNC_REPORT_FILE_NAME),
            requireNotNull(secondFile.parentFile).list()?.sorted(),
        )
    }

    @Test
    fun `failed replacement keeps old report and removes temporary file`() {
        val filesDir = temporaryFilesDir()
        val workingStore = MobileSyncReportStore(filesDir)
        val oldPreview = fullPreview(snapshotAt = "2026-08-02T10:00:00")
        val oldFile = workingStore.write(oldPreview).getOrThrow()
        val oldText = oldFile.readText()
        val failingStore = MobileSyncReportStore(filesDir) { _, _ ->
            error("replace failed")
        }

        val result = failingStore.write(fullPreview(snapshotAt = "2026-08-02T11:00:00"))

        assertTrue(result.isFailure)
        assertEquals(oldText, oldFile.readText())
        assertEquals(oldPreview.snapshotAt, workingStore.latestSnapshotAt())
        assertEquals(
            listOf(MOBILE_SYNC_REPORT_FILE_NAME),
            requireNotNull(oldFile.parentFile).list()?.sorted(),
        )
    }

    @Test
    fun `two report reads return the same file without rebuilding preview`() {
        val store = MobileSyncReportStore(temporaryFilesDir())
        val written = store.write(fullPreview()).getOrThrow()

        val first = assertNotNull(store.latestReport().getOrNull())
        val second = assertNotNull(store.latestReport().getOrNull())

        assertEquals(written.absolutePath, first.absolutePath)
        assertEquals(first.absolutePath, second.absolutePath)
    }
}

private fun fullPreview(
    snapshotAt: String = "2026-08-02T12:30:00",
): MobileSyncPreview = MobileSyncPreview(
    snapshotAt = LocalDateTime.parse(snapshotAt),
    localChanges = listOf(
        MobileExperimentChangeGroup(
            experimentSyncId = "experiment-1",
            title = "Опыт 1",
            summary = "Метаданные 1 · Напоминания 1 · Записи 1 · Файлы 1",
            metadata = MobileEntityChange(
                syncId = "experiment-1",
                title = "Метаданные",
                actionLabel = "Обновление",
                diffs = listOf(MobileFieldDiff("Название", "Старое", "Новое")),
                deleted = false,
            ),
            reminders = listOf(
                MobileEntityChange(
                    syncId = "reminder-1",
                    title = "Позвонить",
                    actionLabel = "Удаление",
                    diffs = listOf(MobileFieldDiff("Напоминание", "Удалено", "Позвонить")),
                    deleted = true,
                )
            ),
            entries = listOf(
                MobileEntryChange(
                    syncId = "entry-1",
                    title = "Запись от 02.08.2026",
                    actionLabel = "Обновление",
                    diffs = listOf(MobileFieldDiff("Текст", "До", "После")),
                    files = listOf(
                        MobileEntityChange(
                            syncId = "file-1",
                            title = "protocol.pdf",
                            actionLabel = "Создание",
                            diffs = listOf(MobileFieldDiff("Имя", "Нет", "protocol.pdf")),
                            deleted = false,
                        )
                    ),
                    deleted = false,
                )
            ),
        )
    ),
    remoteChanges = listOf(
        MobileExperimentChangeGroup(
            experimentSyncId = "experiment-2",
            title = "Опыт 2",
            summary = "Метаданные 1 · Напоминания 0 · Записи 0 · Файлы 0",
            metadata = MobileEntityChange(
                syncId = "experiment-2",
                title = "Метаданные",
                actionLabel = "Создание",
                diffs = listOf(MobileFieldDiff("Название", "Нет", "Опыт 2")),
                deleted = false,
            ),
            reminders = emptyList(),
            entries = emptyList(),
        )
    ),
)

private fun temporaryFilesDir() = Files.createTempDirectory("nocombro-mobile-report-")
    .toFile()
    .apply { deleteOnExit() }
