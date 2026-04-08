package ru.pavlig43.database.data.transact

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

internal const val TRANSACTION_TABLE_NAME = "transact"
@Entity(
    tableName = TRANSACTION_TABLE_NAME,
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class Transact(

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

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,

    ):SingleItem

