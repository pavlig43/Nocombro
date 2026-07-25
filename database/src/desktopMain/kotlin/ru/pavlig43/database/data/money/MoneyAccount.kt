package ru.pavlig43.database.data.money

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val MONEY_ACCOUNT_TABLE_NAME = "money_account"
const val DEFAULT_MONEY_ACCOUNT_SYNC_ID = "money-default-account-v1"

@Entity(
    tableName = MONEY_ACCOUNT_TABLE_NAME,
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class MoneyAccount(
    val name: String,

    @ColumnInfo("account_type")
    val accountType: MoneyAccountType,

    @ColumnInfo("opened_at")
    val openedAt: LocalDateTime,

    @ColumnInfo("is_archived")
    val isArchived: Boolean = false,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : SingleItem

enum class MoneyAccountType {
    CASH,
    BANK,
}
