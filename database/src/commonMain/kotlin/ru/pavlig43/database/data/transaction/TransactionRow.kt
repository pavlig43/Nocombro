package ru.pavlig43.database.data.transaction

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.database.data.product.Product

@Entity(
    tableName = "transaction_row",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
        ),
        ForeignKey(
            entity = DeclarationIn::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
        ),
        ForeignKey(
            entity = ProductTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
        )
    ]
)
data class TransactionRow(

    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("declaration_id")
    val declarationId: Int,

    @ColumnInfo("date_born")
    val dateBorn: Long,

    @ColumnInfo("batch")
    val batch: Int,

    @ColumnInfo("transaction_id")
    val transactionId: Int,

    @ColumnInfo("count")
    val count: Int,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
)

data class TransactionRowOut(
    val productId: Int,
    val productName: String,
    val declarationId: Int,
    val declarationWithVendorName: String,
    val dateBorn: Long,
    val batch: Int,
    val id: Int,
)