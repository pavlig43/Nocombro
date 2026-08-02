package ru.pavlig43.nocombro.mobile.sync

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.datetime.dateTimeFormat

/**
 * Хранит один актуальный Markdown-отчёт проверки Android-синхронизации.
 */
class MobileSyncReportStore(
    private val filesDir: File,
    private val replaceFile: (Path, Path) -> Unit = ::replaceAtomically,
) {
    /**
     * Сначала пишет временный файл, затем атомарно заменяет актуальный отчёт.
     * Ошибка записи или замены не затрагивает старый файл.
     */
    fun write(preview: MobileSyncPreview): Result<File> = runCatching {
        val directory = File(filesDir, REPORT_DIRECTORY_NAME)
        check(directory.isDirectory || directory.mkdirs()) {
            "Не удалось создать каталог отчёта синхронизации."
        }
        val target = File(directory, LATEST_REPORT_FILE_NAME)
        val temporary = Files.createTempFile(
            directory.toPath(),
            TEMPORARY_REPORT_PREFIX,
            TEMPORARY_REPORT_SUFFIX,
        ).toFile()
        try {
            temporary.writeText(
                MobileSyncReportFormatter.format(preview),
                Charsets.UTF_8,
            )
            replaceFile(temporary.toPath(), target.toPath())
        } finally {
            Files.deleteIfExists(temporary.toPath())
        }
        target
    }

    /** Возвращает готовый отчёт без чтения Room, YDB или S3. */
    fun latestReport(): Result<File> = runCatching {
        val report = File(File(filesDir, REPORT_DIRECTORY_NAME), LATEST_REPORT_FILE_NAME)
        require(report.isFile) {
            "Отчёт ещё не создан. Сначала запустите проверку синхронизации."
        }
        report
    }

    /** Читает время снимка из служебной метки Markdown-файла. */
    fun latestSnapshotAt(): LocalDateTime? {
        val report = latestReport().getOrNull() ?: return null
        val marker = report.useLines { lines ->
            lines.firstOrNull { line -> line.startsWith(SNAPSHOT_MARKER_PREFIX) }
        } ?: return null
        return runCatching {
            LocalDateTime.parse(
                marker.removePrefix(SNAPSHOT_MARKER_PREFIX).removeSuffix(SNAPSHOT_MARKER_SUFFIX),
            )
        }.getOrNull()
    }
}

/** Формирует отчёт из предпросмотра, уже построенного проверкой. */
internal object MobileSyncReportFormatter {
    fun format(preview: MobileSyncPreview): String = buildString {
        appendLine("$SNAPSHOT_MARKER_PREFIX${preview.snapshotAt}$SNAPSHOT_MARKER_SUFFIX")
        appendLine("# Изменения синхронизации")
        appendLine()
        appendLine("- Снимок: `${preview.snapshotAt.format(dateTimeFormat)}`")
        appendLine()
        appendLine("> Снимок от ${preview.snapshotAt.format(dateTimeFormat)}. Не обновляется при открытии.")
        appendLine()
        appendChangeSection("К отправке", preview.localChanges)
        appendLine()
        appendChangeSection("К получению", preview.remoteChanges)
        preview.error?.let { error ->
            appendLine()
            appendLine("## Конфликты")
            appendLine()
            appendLine(error.markdownText())
        }
    }.trimEnd() + "\n"

    private fun StringBuilder.appendChangeSection(
        title: String,
        groups: List<MobileExperimentChangeGroup>,
    ) {
        appendLine("## $title")
        appendLine()
        if (groups.isEmpty()) {
            appendLine("Изменений нет.")
            return
        }
        groups.forEachIndexed { index, group ->
            appendExperimentGroup(group)
            if (index != groups.lastIndex) appendLine()
        }
    }

    private fun StringBuilder.appendExperimentGroup(group: MobileExperimentChangeGroup) {
        appendLine("### ${group.title.markdownText()}")
        appendLine()
        appendLine(group.summary.markdownText())
        group.metadata?.let { metadata ->
            appendLine()
            appendLine("#### Опыт")
            appendLine()
            appendEntity(metadata)
        }
        if (group.reminders.isNotEmpty()) {
            appendLine()
            appendLine("#### Напоминания")
            appendLine()
            group.reminders.forEach { reminder -> appendEntity(reminder) }
        }
        if (group.entries.isNotEmpty()) {
            appendLine()
            appendLine("#### Записи")
            appendLine()
            group.entries.forEach { entry -> appendEntry(entry) }
        }
    }

    private fun StringBuilder.appendEntity(change: MobileEntityChange) {
        appendLine("- **${change.title.markdownText()}** — ${change.actionLabel.markdownText()}")
        change.diffs.forEach { diff -> appendDiff(diff, indent = "  ") }
    }

    private fun StringBuilder.appendEntry(change: MobileEntryChange) {
        appendLine("- **${change.title.markdownText()}** — ${change.actionLabel.markdownText()}")
        change.diffs.forEach { diff -> appendDiff(diff, indent = "  ") }
        if (change.files.isNotEmpty()) {
            appendLine("  - Файлы:")
            change.files.forEach { file ->
                appendLine("    - **${file.title.markdownText()}** — ${file.actionLabel.markdownText()}")
                file.diffs.forEach { diff -> appendDiff(diff, indent = "      ") }
            }
        }
    }

    private fun StringBuilder.appendDiff(
        diff: MobileFieldDiff,
        indent: String,
    ) {
        appendLine(
            "$indent- ${diff.label.markdownText()}: " +
                "`${diff.before.markdownCode()}` → `${diff.after.markdownCode()}`"
        )
    }
}

private fun replaceAtomically(source: Path, target: Path) {
    Files.move(source, target, ATOMIC_MOVE, REPLACE_EXISTING)
}

private fun String.markdownText(): String = replace("\\", "\\\\")
    .replace("*", "\\*")
    .replace("_", "\\_")
    .replace("\r\n", "<br>")
    .replace("\n", "<br>")
    .replace("\r", "<br>")

private fun String.markdownCode(): String = markdownText().replace("`", "\\`")

internal const val MOBILE_SYNC_REPORT_FILE_NAME = "latest-sync-analysis.md"
private const val REPORT_DIRECTORY_NAME = "sync-reports"
private const val LATEST_REPORT_FILE_NAME = MOBILE_SYNC_REPORT_FILE_NAME
private const val TEMPORARY_REPORT_PREFIX = ".latest-sync-analysis-"
private const val TEMPORARY_REPORT_SUFFIX = ".tmp"
private const val SNAPSHOT_MARKER_PREFIX = "<!-- sync-snapshot-at="
private const val SNAPSHOT_MARKER_SUFFIX = " -->"
