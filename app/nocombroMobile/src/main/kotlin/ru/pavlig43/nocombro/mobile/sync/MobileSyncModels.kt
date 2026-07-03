package ru.pavlig43.nocombro.mobile.sync

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Подмножество mirror-таблиц, с которым работает Android-клиент.
 */
enum class MobileMirrorTable(
    val tableName: String,
    val order: Int,
) {
    EXPERIMENT("experiment", 0),
    EXPERIMENT_ENTRY("experiment_entry", 1),
    EXPERIMENT_REMINDER("experiment_reminder", 2),
    FILE("file", 3),
}

/**
 * Тип владельца файла из mirror-метаданных десктопа.
 *
 * Android сейчас применяет только файлы записей экспериментов, но читает
 * перечень шире, чтобы не падать на строках `file` из десктопной синхронизации.
 */
enum class MobileFileOwnerType {
    EXPERIMENT_ENTRY,
    DECLARATION,
    PRODUCT,
    VENDOR,
    DOCUMENT,
    TRANSACTION,
    EXPENSE,
}

/**
 * Общий контракт строки мобильного mirror-снимка.
 */
sealed interface MobileMirrorRow {
    val syncId: String
    val updatedAt: LocalDateTime
    val deletedAt: LocalDateTime?
}

/**
 * Строка зеркала для эксперимента.
 */
data class MobileExperimentMirrorRow(
    override val syncId: String,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime?,
) : MobileMirrorRow

/**
 * Строка зеркала для записи эксперимента.
 */
data class MobileExperimentEntryMirrorRow(
    override val syncId: String,
    val experimentSyncId: String,
    val entryDate: LocalDate,
    val createdAt: LocalDateTime,
    val content: String,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime?,
) : MobileMirrorRow

/**
 * Строка зеркала для напоминания эксперимента.
 */
data class MobileExperimentReminderMirrorRow(
    override val syncId: String,
    val experimentSyncId: String,
    val text: String,
    val reminderDateTime: LocalDateTime,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime?,
) : MobileMirrorRow

/**
 * Строка зеркала для метаданных файла.
 *
 * `remoteObjectKey` хранится без S3-префикса. Сам бинарный файл лежит в S3.
 */
data class MobileFileMirrorRow(
    override val syncId: String,
    val ownerType: MobileFileOwnerType,
    val ownerSyncId: String,
    val displayName: String,
    val path: String,
    val remoteObjectKey: String?,
    val remoteStorageProvider: String?,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime?,
) : MobileMirrorRow

/**
 * Снимок локального или удалённого mirror на момент [loadedAt].
 */
data class MobileMirrorSnapshot(
    val loadedAt: LocalDateTime,
    val rowsByTable: Map<MobileMirrorTable, List<MobileMirrorRow>>,
)

/**
 * Одна строка, которую нужно отправить или получить.
 */
data class MobileMirrorChange(
    val table: MobileMirrorTable,
    val row: MobileMirrorRow,
)

/**
 * План синхронизации только для чтения между локальным и удалённым снимками.
 */
data class MobileSyncPlan(
    val pushChanges: List<MobileMirrorChange>,
    val pullChanges: List<MobileMirrorChange>,
)

/**
 * Статус удалённой синхронизации для Android-интерфейса.
 */
data class MobileSyncStatus(
    val configured: Boolean,
    val checkedAt: LocalDateTime,
    val localChanges: Int = 0,
    val remoteChanges: Int = 0,
    val error: String? = null,
)

/**
 * Итог одного действия синхронизации: проверка, отправка, получение
 * или полный цикл.
 */
data class MobileSyncRunResult(
    val status: MobileSyncStatus,
    val pushed: Int = 0,
    val pulled: Int = 0,
    val lastPushAt: LocalDateTime? = null,
    val lastPullAt: LocalDateTime? = null,
    val error: String? = null,
)

/**
 * Группы расхождений для экрана предпросмотра.
 */
data class MobileSyncPreview(
    val localChanges: List<MobileExperimentChangeGroup>,
    val remoteChanges: List<MobileExperimentChangeGroup>,
    val error: String? = null,
)

/**
 * Все изменения одного эксперимента, сгруппированные для интерфейса.
 */
data class MobileExperimentChangeGroup(
    val experimentSyncId: String,
    val title: String,
    val summary: String,
    val metadata: MobileEntityChange?,
    val reminders: List<MobileEntityChange>,
    val entries: List<MobileEntryChange>,
)

/**
 * Отличие одного поля между старым и новым значением.
 */
data class MobileFieldDiff(
    val label: String,
    val before: String,
    val after: String,
)

/**
 * Изменение простой сущности без вложенных файлов.
 */
data class MobileEntityChange(
    val syncId: String,
    val title: String,
    val actionLabel: String,
    val diffs: List<MobileFieldDiff>,
    val deleted: Boolean,
)

/**
 * Изменение записи эксперимента вместе с её файлами.
 */
data class MobileEntryChange(
    val syncId: String,
    val title: String,
    val actionLabel: String,
    val diffs: List<MobileFieldDiff>,
    val files: List<MobileEntityChange>,
    val deleted: Boolean,
)

/**
 * Возвращает версию строки: tombstone побеждает, если он новее `updatedAt`.
 */
fun MobileMirrorRow.versionAt(): LocalDateTime = deletedAt?.takeIf { it > updatedAt } ?: updatedAt
