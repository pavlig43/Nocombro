package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.model.SingleItem

@Entity(
    tableName = "safety_stock",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SafetyStock(
    @ColumnInfo("product_id", index = true)
    val productId: Int,

    @ColumnInfo("reorder_point")
    val reorderPoint: Long,

    @ColumnInfo("order_quantity")
    val orderQuantity: Long,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0
): SingleItem

