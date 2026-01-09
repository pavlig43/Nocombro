package ru.pavlig43.database.data.transaction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.database.data.files.OwnerType

internal const val TRANSACTION_TABLE_NAME = "transact"
@Entity(
    tableName = TRANSACTION_TABLE_NAME
)
data class Transaction(

    @ColumnInfo("transaction_type")
    val transactionType: TransactionType,

    @ColumnInfo("created_at")
    val createdAt: LocalDateTime,

    @ColumnInfo("comment")
    val comment: String,

    @ColumnInfo("is_completed")
    val isCompleted:Boolean,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    ):GenericItem

