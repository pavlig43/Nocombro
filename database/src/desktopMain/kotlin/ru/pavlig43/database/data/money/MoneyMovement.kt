package ru.pavlig43.database.data.money

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val MONEY_MOVEMENT_TABLE_NAME = "money_movement"

@Entity(
    tableName = MONEY_MOVEMENT_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = MoneyAccount::class,
            parentColumns = ["id"],
            childColumns = ["from_account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
        ForeignKey(
            entity = MoneyAccount::class,
            parentColumns = ["id"],
            childColumns = ["to_account_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [
        Index(value = ["sync_id"], unique = true),
        Index(value = ["from_account_id"]),
        Index(value = ["to_account_id"]),
        Index(value = ["occurred_at"]),
    ]
)
data class MoneyMovement(
    val kind: MoneyMovementKind,

    val category: MoneyMovementCategory?,

    val amount: Long,

    @ColumnInfo("occurred_at")
    val occurredAt: LocalDateTime,

    @ColumnInfo("from_account_id")
    val fromAccountId: Int?,

    @ColumnInfo("to_account_id")
    val toAccountId: Int?,

    val counterparty: String = "",

    val comment: String = "",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : SingleItem

enum class MoneyMovementKind {
    OPENING_BALANCE,
    INCOME,
    EXPENSE,
    TRANSFER,
}

enum class MoneyMovementCategory {
    SALE_PAYMENT,
    PURCHASE_PAYMENT,
    BUSINESS_EXPENSE,
    TAX,
    DIVIDEND,
    OWNER_DEPOSIT,
    REFUND,
    OTHER,
}
