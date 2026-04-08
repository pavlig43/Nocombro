package ru.pavlig43.database.data.transact.reminder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.transact.Transact

const val REMINDER_TABLE_NAME = "reminder"

@Entity(
    tableName = REMINDER_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Transact::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class ReminderBD(
    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @ColumnInfo("text")
    val text: String,

    @ColumnInfo("reminder_date_time")
    val reminderDateTime: LocalDateTime,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
,
    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : CollectionObject
