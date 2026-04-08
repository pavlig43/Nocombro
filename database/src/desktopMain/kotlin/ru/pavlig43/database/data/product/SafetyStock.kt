package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

@Entity(
    tableName = "safety_stock",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sync_id"], unique = true)]
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
,
    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
): SingleItem

