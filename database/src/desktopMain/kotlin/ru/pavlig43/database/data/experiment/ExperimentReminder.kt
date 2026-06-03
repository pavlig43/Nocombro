package ru.pavlig43.database.data.experiment

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val EXPERIMENT_REMINDER_TABLE_NAME = "experiment_reminder"

@Entity(
    tableName = EXPERIMENT_REMINDER_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Experiment::class,
            parentColumns = ["id"],
            childColumns = ["experiment_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["experiment_id"]),
    ]
)
data class ExperimentReminder(
    @ColumnInfo("experiment_id")
    val experimentId: Int,

    @ColumnInfo("text")
    val text: String,

    @ColumnInfo("reminder_date_time")
    val reminderDateTime: LocalDateTime,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : CollectionObject
