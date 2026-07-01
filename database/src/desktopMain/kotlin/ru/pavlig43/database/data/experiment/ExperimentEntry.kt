package ru.pavlig43.database.data.experiment

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val EXPERIMENT_ENTRY_TABLE_NAME = "experiment_entry"

@Entity(
    tableName = EXPERIMENT_ENTRY_TABLE_NAME,
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
data class ExperimentEntry(
    @ColumnInfo("experiment_id")
    val experimentId: Int,

    @ColumnInfo("entry_date")
    val entryDate: LocalDate,

    @ColumnInfo("created_at")
    val createdAt: LocalDateTime = defaultUpdatedAt(),

    val content: String = "",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : SingleItem
