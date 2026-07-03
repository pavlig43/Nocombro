package ru.pavlig43.nocombro.mobile.sync

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.datetime.dateTimeFormat

/**
 * Группирует raw sync changes по экспериментам для экрана preview.
 */
internal fun buildExperimentChangeGroups(
    changes: List<MobileMirrorChange>,
    before: MobileMirrorSnapshot,
    after: MobileMirrorSnapshot,
): List<MobileExperimentChangeGroup> {
    val beforeIndex = before.toRowIndex()
    val afterIndex = after.toRowIndex()
    val beforeEntries = before.entriesBySyncId()
    val afterEntries = after.entriesBySyncId()
    val knownExperimentIds = before.experimentSyncIds() + after.experimentSyncIds()
    val experimentIds = linkedSetOf<String>()
    val metadata = linkedMapOf<String, MobileEntityChange>()
    val reminders = linkedMapOf<String, MutableList<MobileEntityChange>>()
    val entries = linkedMapOf<String, MutableMap<String, EntryAccumulator>>()

    fun includeExperiment(experimentSyncId: String): String? {
        if (experimentSyncId !in knownExperimentIds) return null
        experimentIds += experimentSyncId
        return experimentSyncId
    }

    fun entryAccumulator(entrySyncId: String): EntryAccumulator? {
        val beforeEntry = beforeEntries[entrySyncId]
        val afterEntry = afterEntries[entrySyncId]
        val experimentSyncId = includeExperiment(
            afterEntry?.experimentSyncId
                ?: beforeEntry?.experimentSyncId
                ?: return null,
        ) ?: return null
        val byEntry = entries.getOrPut(experimentSyncId) { linkedMapOf() }
        return byEntry.getOrPut(entrySyncId) {
            EntryAccumulator(
                before = beforeEntry,
                after = afterEntry,
            )
        }
    }

    changes.forEach { change ->
        val rowBefore = beforeIndex[EntityKey(change.table, change.row.syncId)]
        val rowAfter = afterIndex[EntityKey(change.table, change.row.syncId)] ?: change.row
        when (change.table) {
            MobileMirrorTable.EXPERIMENT -> {
                experimentIds += change.row.syncId
                val experimentSyncId = change.row.syncId
                metadata[experimentSyncId] = buildExperimentChange(
                    before = rowBefore as? MobileExperimentMirrorRow,
                    after = rowAfter as? MobileExperimentMirrorRow,
                )
            }
            MobileMirrorTable.EXPERIMENT_REMINDER -> {
                val beforeReminder = rowBefore as? MobileExperimentReminderMirrorRow
                val afterReminder = rowAfter as? MobileExperimentReminderMirrorRow
                val experimentSyncId = includeExperiment(
                    afterReminder?.experimentSyncId
                        ?: beforeReminder?.experimentSyncId
                        ?: return@forEach,
                ) ?: return@forEach
                reminders.getOrPut(experimentSyncId) { mutableListOf() } += buildReminderChange(
                    before = beforeReminder,
                    after = afterReminder,
                )
            }
            MobileMirrorTable.EXPERIMENT_ENTRY -> {
                val beforeEntry = rowBefore as? MobileExperimentEntryMirrorRow
                val afterEntry = rowAfter as? MobileExperimentEntryMirrorRow
                val entrySyncId = rowAfter.syncId
                val accumulator = entryAccumulator(entrySyncId) ?: return@forEach
                accumulator.before = beforeEntry ?: accumulator.before
                accumulator.after = afterEntry ?: accumulator.after
            }
            MobileMirrorTable.FILE -> {
                val beforeFile = rowBefore as? MobileFileMirrorRow
                val afterFile = rowAfter as? MobileFileMirrorRow
                val entrySyncId = afterFile?.ownerSyncId ?: beforeFile?.ownerSyncId ?: return@forEach
                val accumulator = entryAccumulator(entrySyncId) ?: return@forEach
                accumulator.files += buildFileChange(
                    before = beforeFile,
                    after = afterFile,
                )
            }
        }
    }

    return experimentIds.map { experimentSyncId ->
        val entryChanges = entries[experimentSyncId].orEmpty().values
            .map(EntryAccumulator::toChange)
            .sortedBy(MobileEntryChange::title)
        val reminderChanges = reminders[experimentSyncId].orEmpty()
            .sortedBy(MobileEntityChange::title)
        val metadataChange = metadata[experimentSyncId]
        MobileExperimentChangeGroup(
            experimentSyncId = experimentSyncId,
            title = titleForExperiment(experimentSyncId, beforeIndex, afterIndex),
            summary = summaryText(
                metadataCount = metadataChange?.diffs?.size ?: 0,
                reminderCount = reminderChanges.size,
                entryCount = entryChanges.size,
                fileCount = entryChanges.sumOf { entry -> entry.files.size },
            ),
            metadata = metadataChange,
            reminders = reminderChanges,
            entries = entryChanges,
        )
    }.filter { group ->
        group.metadata != null || group.reminders.isNotEmpty() || group.entries.isNotEmpty()
    }.sortedBy(MobileExperimentChangeGroup::title)
}

/**
 * Ключ строки внутри snapshot index.
 */
private data class EntityKey(
    val table: MobileMirrorTable,
    val syncId: String,
)

/**
 * Копит diff записи и связанных файлов перед сборкой [MobileEntryChange].
 */
private data class EntryAccumulator(
    var before: MobileExperimentEntryMirrorRow?,
    var after: MobileExperimentEntryMirrorRow?,
    val files: MutableList<MobileEntityChange> = mutableListOf(),
) {
    /**
     * Собирает итоговую UI-модель изменения записи.
     */
    fun toChange(): MobileEntryChange {
        val row = after ?: before
        val deleted = after?.deletedAt != null
        val action = actionLabel(before, after)
        val diffs = when {
            before == null && after != null -> listOf(
                MobileFieldDiff(
                    label = "Запись",
                    before = "Новая запись",
                    after = entryValue(after),
                )
            )
            deleted -> listOf(
                MobileFieldDiff(
                    label = "Запись",
                    before = "Удалено",
                    after = entryValue(after ?: before),
                )
            )
            else -> buildDiffs(
                field("Дата", before?.entryDate?.displayText(), after?.entryDate?.displayText()),
                field("Создано", before?.createdAt?.displayText(), after?.createdAt?.displayText()),
                field("Текст", before?.content, after?.content),
            )
        }
        return MobileEntryChange(
            syncId = row?.syncId.orEmpty(),
            title = row?.let { "Запись от ${it.entryDate.displayText()}" } ?: "Запись",
            actionLabel = action,
            diffs = diffs,
            files = files.sortedBy(MobileEntityChange::title),
            deleted = deleted,
        )
    }
}

private fun MobileMirrorSnapshot.toRowIndex(): Map<EntityKey, MobileMirrorRow> {
    return rowsByTable.flatMap { (table, rows) ->
        rows.map { row -> EntityKey(table, row.syncId) to row }
    }.toMap()
}

private fun MobileMirrorSnapshot.entriesBySyncId(): Map<String, MobileExperimentEntryMirrorRow> {
    return rowsByTable[MobileMirrorTable.EXPERIMENT_ENTRY].orEmpty()
        .filterIsInstance<MobileExperimentEntryMirrorRow>()
        .associateBy(MobileExperimentEntryMirrorRow::syncId)
}

private fun MobileMirrorSnapshot.experimentSyncIds(): Set<String> {
    return rowsByTable[MobileMirrorTable.EXPERIMENT].orEmpty()
        .mapTo(mutableSetOf(), MobileMirrorRow::syncId)
}

private fun buildExperimentChange(
    before: MobileExperimentMirrorRow?,
    after: MobileExperimentMirrorRow?,
): MobileEntityChange {
    val row = after ?: before
    return MobileEntityChange(
        syncId = row?.syncId.orEmpty(),
        title = "Метаданные",
        actionLabel = actionLabel(before, after),
        diffs = if (after?.deletedAt != null) {
            listOf(MobileFieldDiff("Эксперимент", "Удалено", after.title.ifBlank { "Без названия" }))
        } else {
            buildDiffs(
                field("Название", before?.title, after?.title),
                field("Идея", before?.ideaDescription, after?.ideaDescription),
                field("Архив", before?.isArchived?.yesNo(), after?.isArchived?.yesNo()),
            )
        },
        deleted = after?.deletedAt != null,
    )
}

private fun buildReminderChange(
    before: MobileExperimentReminderMirrorRow?,
    after: MobileExperimentReminderMirrorRow?,
): MobileEntityChange {
    val row = after ?: before
    val deleted = after?.deletedAt != null
    return MobileEntityChange(
        syncId = row?.syncId.orEmpty(),
        title = row?.text?.ifBlank { "Напоминание" } ?: "Напоминание",
        actionLabel = actionLabel(before, after),
        diffs = if (deleted) {
            listOf(MobileFieldDiff("Напоминание", "Удалено", reminderValue(row)))
        } else {
            buildDiffs(
                field("Текст", before?.text, after?.text),
                field("Когда", before?.reminderDateTime?.displayText(), after?.reminderDateTime?.displayText()),
            )
        },
        deleted = deleted,
    )
}

private fun buildFileChange(
    before: MobileFileMirrorRow?,
    after: MobileFileMirrorRow?,
): MobileEntityChange {
    val row = after ?: before
    val deleted = after?.deletedAt != null
    return MobileEntityChange(
        syncId = row?.syncId.orEmpty(),
        title = row?.displayName?.ifBlank { "Файл" } ?: "Файл",
        actionLabel = actionLabel(before, after),
        diffs = if (deleted) {
            listOf(MobileFieldDiff("Файл", "Удалено", fileValue(row)))
        } else {
            buildDiffs(
                field("Имя", before?.displayName, after?.displayName),
            )
        },
        deleted = deleted,
    )
}

private fun titleForExperiment(
    experimentSyncId: String,
    beforeIndex: Map<EntityKey, MobileMirrorRow>,
    afterIndex: Map<EntityKey, MobileMirrorRow>,
): String {
    val after = afterIndex[EntityKey(MobileMirrorTable.EXPERIMENT, experimentSyncId)] as? MobileExperimentMirrorRow
    val before = beforeIndex[EntityKey(MobileMirrorTable.EXPERIMENT, experimentSyncId)] as? MobileExperimentMirrorRow
    return after?.title?.ifBlank { null }
        ?: before?.title?.ifBlank { null }
        ?: "Эксперимент ${experimentSyncId.shortId()}"
}

private fun summaryText(
    metadataCount: Int,
    reminderCount: Int,
    entryCount: Int,
    fileCount: Int,
): String {
    return "Метаданные $metadataCount · Напоминания $reminderCount · Записи $entryCount · Файлы $fileCount"
}

private fun actionLabel(
    before: MobileMirrorRow?,
    after: MobileMirrorRow?,
): String {
    return when {
        after?.deletedAt != null -> "Удаление"
        before == null -> "Создание"
        else -> "Обновление"
    }
}

private data class FieldValue(
    val label: String,
    val before: String?,
    val after: String?,
)

private fun field(
    label: String,
    before: String?,
    after: String?,
) = FieldValue(label, before, after)

private fun buildDiffs(vararg values: FieldValue): List<MobileFieldDiff> {
    return values.mapNotNull { value ->
        val before = value.before.orEmpty()
        val after = value.after.orEmpty()
        if (before == after) return@mapNotNull null
        MobileFieldDiff(
            label = value.label,
            before = before.ifBlank { "Нет" },
            after = after.ifBlank { "Нет" },
        )
    }
}

private fun entryValue(row: MobileExperimentEntryMirrorRow?): String {
    if (row == null) return "Нет данных"
    return "${row.entryDate.displayText()} · ${row.content.ifBlank { "Без текста" }}"
}

private fun reminderValue(row: MobileExperimentReminderMirrorRow?): String {
    if (row == null) return "Нет данных"
    return "${row.reminderDateTime.displayText()} · ${row.text.ifBlank { "Без текста" }}"
}

private fun fileValue(row: MobileFileMirrorRow?): String {
    if (row == null) return "Нет данных"
    return row.displayName.ifBlank { row.remoteObjectKey ?: "Файл" }
}

private fun Boolean.yesNo(): String = if (this) "Да" else "Нет"

private fun LocalDate.displayText(): String = format(dateFormat)

private fun LocalDateTime.displayText(): String = format(dateTimeFormat)

private fun String.shortId(): String = take(8)
