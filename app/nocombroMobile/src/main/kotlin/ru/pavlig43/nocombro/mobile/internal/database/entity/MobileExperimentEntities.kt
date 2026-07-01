package ru.pavlig43.nocombro.mobile.internal.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperiment
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentEntry
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentEntryFile
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentReminder

const val EXPERIMENT_TABLE_NAME = "experiment"
const val EXPERIMENT_ENTRY_TABLE_NAME = "experiment_entry"
const val EXPERIMENT_ENTRY_FILE_TABLE_NAME = "mobile_entry_file"
const val EXPERIMENT_REMINDER_TABLE_NAME = "experiment_reminder"

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
    ],
)
data class MobileExperimentEntryEntity(
    @ColumnInfo("experiment_id")
    val experimentId: Int,

    @ColumnInfo("entry_date")
    val entryDate: LocalDate,

    @ColumnInfo("created_at")
    val createdAt: LocalDateTime,

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

@Entity(
    tableName = EXPERIMENT_ENTRY_FILE_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = MobileExperimentEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["entry_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["entry_id"]),
        Index(value = ["object_key"], unique = true),
    ],
)
data class MobileExperimentEntryFileEntity(
    @ColumnInfo("entry_id")
    val entryId: Int,

    @ColumnInfo("display_name")
    val displayName: String,

    @ColumnInfo("local_path")
    val localPath: String,

    @ColumnInfo("object_key")
    val objectKey: String,

    @ColumnInfo("mime_type")
    val mimeType: String? = null,

    @ColumnInfo("size_bytes")
    val sizeBytes: Long? = null,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String,

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime,

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
)

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

fun MobileExperimentEntity.toModel(): MobileExperiment = MobileExperiment(
    id = id,
    syncId = syncId,
    title = title,
    ideaDescription = ideaDescription,
    isArchived = isArchived,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun MobileExperimentEntryEntity.toModel(): MobileExperimentEntry = MobileExperimentEntry(
    id = id,
    syncId = syncId,
    experimentId = experimentId,
    entryDate = entryDate,
    createdAt = createdAt,
    content = content,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun MobileExperimentEntryFileEntity.toModel(): MobileExperimentEntryFile = MobileExperimentEntryFile(
    id = id,
    syncId = syncId,
    entryId = entryId,
    displayName = displayName,
    localPath = localPath,
    objectKey = objectKey,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

fun MobileExperimentReminderEntity.toModel(): MobileExperimentReminder = MobileExperimentReminder(
    id = id,
    syncId = syncId,
    experimentId = experimentId,
    text = text,
    reminderDateTime = reminderDateTime,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
