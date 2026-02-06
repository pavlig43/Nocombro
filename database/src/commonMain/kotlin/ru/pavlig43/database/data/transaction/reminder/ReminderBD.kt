package ru.pavlig43.database.data.transaction.reminder

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.transaction.Transaction

internal const val REMINDER_TABLE_NAME = "reminder"

@Entity(
    tableName = REMINDER_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ReminderBD(
    @ColumnInfo("transaction_id")
    val transactionId: Int,

    @ColumnInfo("text")
    val text: String,

    @ColumnInfo("reminder_date_time")
    val reminderDateTime: LocalDateTime,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
) : CollectionObject
