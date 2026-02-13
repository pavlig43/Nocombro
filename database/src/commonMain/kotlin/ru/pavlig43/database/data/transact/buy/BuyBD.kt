package ru.pavlig43.database.data.transact.buy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transact.Transact

const val BUY_TABLE_NAME = "buy"

@Entity(
    tableName = BUY_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Transact::class,
            parentColumns = ["id"],
            childColumns = ["transaction_id"],
            onDelete = ForeignKey.CASCADE
        ),

    ]
)
data class BuyBDIn(
    @ColumnInfo("transaction_id", index = true)
    val transactionId: Int,

    @ColumnInfo("batch_id")
    val batchId: Int,

    @ColumnInfo("price")
    val price: Int, // Цена в копейках

    @ColumnInfo("count")
    val count: Int, // Количество

    @ColumnInfo("comment")
    val comment: String,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
) : CollectionObject






data class BuyBDOut(
    val transactionId: Int,
    val count: Int,
    val batchId: Int,
    val productId: Int,
    val productName: String,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val price: Int,
    val comment: String,
    override val id: Int
) : CollectionObject
