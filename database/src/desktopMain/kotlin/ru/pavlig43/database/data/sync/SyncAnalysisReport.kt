package ru.pavlig43.database.data.sync

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorReconciliationPreview
import ru.pavlig43.database.data.sync.mirror.MirrorSyncRow
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.dateTimeFormat
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * Создаёт и сохраняет анализ расхождений Room/YDB в Markdown без изменения данных.
 */
class SyncAnalysisReportWriter(
    private val reportDirectory: () -> File = ::defaultSyncReportDirectory,
    private val json: Json = Json { classDiscriminator = "_mirrorType" },
) {
    /**
     * Атомарно заменяет один актуальный отчёт. Старые отчёты с датой в имени
     * остаются в каталоге без изменений.
     */
    fun write(preview: MirrorReconciliationPreview): File {
        val directory = reportDirectory()
        Files.createDirectories(directory.toPath())
        val target = directory.toPath().resolve(LATEST_REPORT_FILE_NAME)
        val temporary = Files.createTempFile(
            directory.toPath(),
            ".latest-sync-analysis-",
            ".tmp",
        )
        try {
            Files.writeString(
                temporary,
                SyncAnalysisReportFormatter(json).format(preview),
                StandardCharsets.UTF_8,
            )
            Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
        return target.toFile()
    }

    /** Возвращает уже записанный отчёт без чтения Room или YDB. */
    fun latestReport(): Result<File> = runCatching {
        val file = File(reportDirectory(), LATEST_REPORT_FILE_NAME)
        require(file.isFile) {
            "Отчёт ещё не создан. Сначала обновите статус синхронизации."
        }
        file
    }

    /** Читает время снимка из актуального файла без сетевых запросов. */
    @Suppress("ReturnCount")
    fun latestSnapshotAt(): LocalDateTime? {
        val report = latestReport().getOrNull() ?: return null
        val marker = report.useLines { lines ->
            lines.firstOrNull { it.startsWith(SNAPSHOT_MARKER_PREFIX) }
        } ?: return null
        return runCatching {
            LocalDateTime.parse(
                marker.removePrefix(SNAPSHOT_MARKER_PREFIX).removeSuffix(SNAPSHOT_MARKER_SUFFIX)
            )
        }.getOrNull()
    }
}

internal class SyncAnalysisReportFormatter(
    private val json: Json = Json { classDiscriminator = "_mirrorType" },
) {
    fun format(preview: MirrorReconciliationPreview): String = buildString {
        appendLine("$SNAPSHOT_MARKER_PREFIX${preview.remoteSnapshot.loadedAt}$SNAPSHOT_MARKER_SUFFIX")
        appendLine("# Отчёт синхронизации")
        appendLine()
        appendLine("- Snapshot Room: `${preview.localSnapshot.loadedAt.format(dateTimeFormat)}`")
        appendLine("- Snapshot YDB: `${preview.remoteSnapshot.loadedAt.format(dateTimeFormat)}`")
        appendLine()
        appendLine("> Состояние Room и YDB могло измениться после формирования этого отчёта.")
        appendLine(
            "> Снимок от ${preview.remoteSnapshot.loadedAt.format(dateTimeFormat)}. " +
                "Не обновляется при открытии."
        )
        appendLine()
        appendSection(
            title = "Будет отправлено",
            changes = preview.plan.pushChanges,
            targetRows = preview.remoteSnapshot.rowsByTable,
        )
        appendLine()
        appendSection(
            title = "Будет получено",
            changes = preview.plan.pullChanges,
            targetRows = preview.localSnapshot.rowsByTable,
        )
        appendLine()
        appendConflicts(preview.plan.conflicts)
    }.trimEnd() + "\n"

    private fun StringBuilder.appendConflicts(
        conflicts: List<MirrorVersionConflict>,
    ) {
        appendLine("## Конфликты")
        appendLine()
        if (conflicts.isEmpty()) {
            appendLine("Конфликтов нет.")
            return
        }

        appendLine("| Таблица | syncId | Локальная версия | Удалённая версия |")
        appendLine("|---|---|---|---|")
        conflicts.forEach { conflict ->
            appendLine(
                "| " + escapeCell(conflict.table.tableName) +
                    " | " + escapeCell(conflict.localRow.syncId) +
                    " | " + escapeCell(conflict.localRow.versionText()) +
                    " | " + escapeCell(conflict.remoteRow.versionText()) + " |"
            )
        }
    }

    private fun StringBuilder.appendSection(
        title: String,
        changes: List<MirrorPushEntityChange>,
        targetRows: Map<MirrorSyncTable, List<MirrorSyncRow>>,
    ) {
        appendLine("## $title")
        appendLine()
        if (changes.isEmpty()) {
            appendLine("Расхождений нет.")
            return
        }

        changes.forEachIndexed { index, change ->
            val previous = targetRows[change.table].orEmpty()
                .firstOrNull { it.syncId == change.row.syncId }
            appendChange(index + 1, change, previous)
            if (index != changes.lastIndex) appendLine()
        }
    }

    private fun StringBuilder.appendChange(
        number: Int,
        change: MirrorPushEntityChange,
        previous: MirrorSyncRow?,
    ) {
        val operation = operation(previous, change.row)
        appendLine("### $number. `${escapeInline(change.table.tableName)}` / `${escapeInline(change.row.syncId)}`")
        appendLine()
        appendLine("| Таблица | syncId | Операция | Старая версия | Новая версия |")
        appendLine("|---|---|---|---|---|")
        appendLine(
            "| ${escapeCell(change.table.tableName)} " +
                "| ${escapeCell(change.row.syncId)} " +
                "| $operation " +
                "| ${escapeCell(previous?.versionText() ?: "отсутствует")} " +
                "| ${escapeCell(change.row.versionText())} |"
        )
        appendLine()
        appendLine("| Поле | Изменение |")
        appendLine("|---|---|")
        val differences = payloadDifferences(previous, change.row)
        if (differences.isEmpty()) {
            appendLine("| _Изменений payload нет_ |  |")
        } else {
            differences.forEach { difference ->
                appendLine(
                    "| ${escapeCell(difference.field)} " +
                        "| ${escapeCell(difference.oldValue)} → ${escapeCell(difference.newValue)} |"
                )
            }
        }
    }

    private fun payloadDifferences(
        previous: MirrorSyncRow?,
        current: MirrorSyncRow,
    ): List<FieldDifference> {
        val oldPayload = previous?.payload() ?: emptyMap()
        val newPayload = current.payload()
        return (oldPayload.keys + newPayload.keys)
            .sorted()
            .mapNotNull { field ->
                val oldValue = oldPayload[field] ?: JsonNull
                val newValue = newPayload[field] ?: JsonNull
                if (oldValue == newValue) null else {
                    FieldDifference(field, oldValue.reportValue(), newValue.reportValue())
                }
            }
    }

    private fun MirrorSyncRow.payload(): JsonObject {
        val encoded = json.encodeToJsonElement<MirrorSyncRow>(this) as JsonObject
        return JsonObject(encoded.filterKeys { it !in EXCLUDED_PAYLOAD_FIELDS })
    }

    private fun MirrorSyncRow.versionText(): String =
        (deletedAt?.takeIf { it > updatedAt } ?: updatedAt).format(dateTimeFormat)

    private fun operation(previous: MirrorSyncRow?, current: MirrorSyncRow): String = when {
        current.deletedAt != null -> "удаление"
        previous == null || previous.deletedAt != null -> "создание"
        else -> "изменение"
    }

    private fun JsonElement.reportValue(): String = when (this) {
        JsonNull -> "null"
        is JsonPrimitive -> if (isString) content.formatSerializedDateOrTime() else toString()
        else -> toString()
    }
}

private fun String.formatSerializedDateOrTime(): String {
    return runCatching { LocalDateTime.parse(this).format(dateTimeFormat) }
        .recoverCatching { LocalDate.parse(this).format(dateFormat) }
        .getOrDefault(this)
}

private data class FieldDifference(
    val field: String,
    val oldValue: String,
    val newValue: String,
)

private fun escapeCell(value: String): String = value
    .replace("\\", "\\\\")
    .replace("|", "\\|")
    .replace("\r\n", "<br>")
    .replace("\n", "<br>")
    .replace("\r", "<br>")

private fun escapeInline(value: String): String = value.replace("`", "\\`")

private fun defaultSyncReportDirectory(): File {
    val appData = System.getenv("APPDATA")
        ?.takeIf(String::isNotBlank)
        ?: File(System.getProperty("user.home"), "AppData/Roaming").path
    return File(appData, "Nocombro/sync-reports")
}

private const val LATEST_REPORT_FILE_NAME = "latest-sync-analysis.md"
private const val SNAPSHOT_MARKER_PREFIX = "<!-- sync-snapshot-at="
private const val SNAPSHOT_MARKER_SUFFIX = " -->"
private val EXCLUDED_PAYLOAD_FIELDS = setOf("syncId", "updatedAt", "deletedAt", "_mirrorType")
