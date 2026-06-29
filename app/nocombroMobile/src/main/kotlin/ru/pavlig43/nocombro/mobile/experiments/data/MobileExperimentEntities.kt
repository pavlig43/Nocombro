package ru.pavlig43.nocombro.mobile.experiments.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.MobileExperiment
import ru.pavlig43.nocombro.mobile.experiments.MobileExperimentEntry
import ru.pavlig43.nocombro.mobile.experiments.MobileExperimentReminder

/**
 * Имя таблицы экспериментов.
 */
const val EXPERIMENT_TABLE_NAME = "experiment"

/**
 * Имя таблицы записей журнала эксперимента.
 */
const val EXPERIMENT_ENTRY_TABLE_NAME = "experiment_entry"

/**
 * Имя таблицы напоминаний по эксперименту.
 */
const val EXPERIMENT_REMINDER_TABLE_NAME = "experiment_reminder"

/**
 * Room entity эксперимента в mobile-БД.
 */
@Entity(
    tableName = EXPERIMENT_TABLE_NAME,
    indices = [Index(value = ["sync_id"], unique = true)],
)
data class MobileExperimentEntity(
    val title: String = "",

    @ColumnInfo("idea_description")
    val ideaDescription: String = "",

    @ColumnInfo("is_archived")
    val isArchived: Boolean = false,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String,

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime,

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
)

/**
 * Room entity записи журнала эксперимента.
 */
@Entity(
    tableName = EXPERIMENT_ENTRY_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = MobileExperimentEntity::class,
            parentColumns = ["id"],
            childColumns = ["experiment_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["experiment_id"]),
        Index(value = ["experiment_id", "entry_date"], unique = true),
    ],
)
data class MobileExperimentEntryEntity(
    @ColumnInfo("experiment_id")
    val experimentId: Int,

    @ColumnInfo("entry_date")
    val entryDate: LocalDate,

    val content: String = "",

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String,

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime,

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
)

/**
 * Room entity напоминания по эксперименту.
 */
@Entity(
    tableName = EXPERIMENT_REMINDER_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = MobileExperimentEntity::class,
            parentColumns = ["id"],
            childColumns = ["experiment_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["experiment_id"]),
    ],
)
data class MobileExperimentReminderEntity(
    @ColumnInfo("experiment_id")
    val experimentId: Int,

    @ColumnInfo("text")
    val text: String,

    @ColumnInfo("reminder_date_time")
    val reminderDateTime: LocalDateTime,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String,

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime,

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
)

/**
 * Преобразует Room entity эксперимента в UI-модель.
 */
fun MobileExperimentEntity.toModel(): MobileExperiment = MobileExperiment(
    id = id,
    syncId = syncId,
    title = title,
    ideaDescription = ideaDescription,
    isArchived = isArchived,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/**
 * Преобразует Room entity записи журнала в UI-модель.
 */
fun MobileExperimentEntryEntity.toModel(): MobileExperimentEntry = MobileExperimentEntry(
    id = id,
    syncId = syncId,
    experimentId = experimentId,
    entryDate = entryDate,
    content = content,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/**
 * Преобразует Room entity напоминания в UI-модель.
 */
fun MobileExperimentReminderEntity.toModel(): MobileExperimentReminder = MobileExperimentReminder(
    id = id,
    syncId = syncId,
    experimentId = experimentId,
    text = text,
    reminderDateTime = reminderDateTime,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
