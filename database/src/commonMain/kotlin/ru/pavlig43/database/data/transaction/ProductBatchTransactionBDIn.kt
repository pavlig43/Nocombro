package ru.pavlig43.database.data.transaction

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.ForeignKey.Companion.RESTRICT
import androidx.room.PrimaryKey
import androidx.room.Relation
import ru.pavlig43.core.data.CollectionObject
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product

@Entity(
    tableName = "product_batch_transaction",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = RESTRICT
        ),
        ForeignKey(
            entity = Declaration::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
            onDelete = RESTRICT
        ),
        ForeignKey(
            entity = ProductTransaction::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = CASCADE
        )
    ]
)
data class ProductBatchTransactionBDIn(

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
    override val id: Int = 0
): CollectionObject


data class ProductBatchTransactionBDOut(
    val productId: Int,
    val productName: String,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val dateBorn: Long,
    val batch: Int,
    override val id: Int,
): CollectionObject
