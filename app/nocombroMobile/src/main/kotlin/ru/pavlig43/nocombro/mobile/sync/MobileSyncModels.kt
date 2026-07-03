package ru.pavlig43.nocombro.mobile.sync

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Подмножество mirror tables, с которым работает Android-клиент.
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
 * Тип владельца файла из desktop mirror metadata.
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
 * Общий contract строки mobile mirror snapshot.
 */
sealed interface MobileMirrorRow {
    val syncId: String
    val updatedAt: LocalDateTime
    val deletedAt: LocalDateTime?
}

/**
 * Mirror row эксперимента.
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
 * Mirror row записи эксперимента.
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
 * Mirror row напоминания эксперимента.
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
 * Mirror row metadata файла; сам binary хранится в S3.
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
 * Snapshot local или remote mirror на момент [loadedAt].
 */
data class MobileMirrorSnapshot(
    val loadedAt: LocalDateTime,
    val rowsByTable: Map<MobileMirrorTable, List<MobileMirrorRow>>,
)

/**
 * Одна строка, которую нужно push или pull.
 */
data class MobileMirrorChange(
    val table: MobileMirrorTable,
    val row: MobileMirrorRow,
)

/**
 * Read-only план sync между local и remote snapshots.
 */
data class MobileSyncPlan(
    val pushChanges: List<MobileMirrorChange>,
    val pullChanges: List<MobileMirrorChange>,
)

/**
 * Runtime-статус remote sync для Android UI.
 */
data class MobileSyncStatus(
    val configured: Boolean,
    val checkedAt: LocalDateTime,
    val localChanges: Int = 0,
    val remoteChanges: Int = 0,
    val error: String? = null,
)

/**
 * Итог одного sync-действия: check, push, pull или full sync.
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
 * Группы расхождений для экрана preview.
 */
data class MobileSyncPreview(
    val localChanges: List<MobileExperimentChangeGroup>,
    val remoteChanges: List<MobileExperimentChangeGroup>,
    val error: String? = null,
)

/**
 * Все изменения одного эксперимента, сгруппированные для UI.
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
 * Отличие одного поля между before и after.
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
