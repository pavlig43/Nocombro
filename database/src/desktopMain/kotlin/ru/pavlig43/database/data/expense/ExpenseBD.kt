package ru.pavlig43.database.data.expense

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.transact.Transact

const val EXPENSE_TABLE_NAME = "expense"

@Entity(
    tableName = EXPENSE_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Transact::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["transaction_id"]),
        Index(value = ["sync_id"], unique = true),
    ]
)
data class ExpenseBD(
    /**
     * ID транзакции (может быть null - расход не привязан к транзакции)
     */
    @ColumnInfo("transaction_id")
    val transactionId: Int?,

    /**
     * Тип расхода
     */
    @ColumnInfo("expense_type")
    val expenseType: ExpenseType,

    /**
     * Сумма расхода в копейках
     */
    @ColumnInfo("amount")
    val amount: Long,

    /**
     * Дата и время расхода
     */
    @ColumnInfo("expense_date_time")
    val expenseDateTime: LocalDateTime,

    /**
     * Комментарий
     */
    @ColumnInfo("comment")
    val comment: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
,
    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : CollectionObject, SingleItem
