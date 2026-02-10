package ru.pavlig43.database.data.transaction.buy

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transaction.Transact

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
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Declaration::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]
)
data class BuyBDIn(
    @ColumnInfo("transaction_id")
    val transactionId: Int,

    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("declaration_id")
    val declarationId: Int,

    @ColumnInfo("date_born")
    val dateBorn: LocalDate, // Дата производства партии

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
    val productName: String,
    val count: Int,
    val declarationName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val price: Int,
    val comment: String,
    override val id: Int
) : CollectionObject
