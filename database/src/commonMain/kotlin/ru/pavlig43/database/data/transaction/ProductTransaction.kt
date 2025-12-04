package ru.pavlig43.database.data.transaction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.GenericItem

@Entity(
    tableName = "product_transaction"
)
data class ProductTransaction(

    @ColumnInfo("transaction_type")
    val transactionType: TransactionType,

    @ColumnInfo("operation_type")
    val operationType: OperationType,

    @ColumnInfo("date")
    val date: Long,

    @ColumnInfo("comment")
    val comment: String,

    @ColumnInfo("is_completed")
    val isCompleted:Boolean,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

):GenericItem{
    @Ignore
    override val displayName: String = "stub"
}

data class ProductTransactionIn(
    val transactionForSave:ProductTransaction,
    val products:List<TransactionRow>
):GenericItem by transactionForSave

data class ProductTransactionOut(
    val transaction:ProductTransaction,
    val productRows:List<TransactionRowOut>
):GenericItem by transaction
