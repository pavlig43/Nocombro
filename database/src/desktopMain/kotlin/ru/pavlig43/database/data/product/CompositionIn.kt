package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt


@Entity(
    tableName = "composition",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["parent_id"],
            onDelete = ForeignKey.RESTRICT,
        )
    ],
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class CompositionIn(
    @PrimaryKey(autoGenerate = true)
    override val id: Int,

    @ColumnInfo("parent_id", index = true)
    val parentId: Int,

    @ColumnInfo("product_id", index = true)
    val productId: Int,

    val count: Long,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : CollectionObject

data class CompositionOut(
    override val id: Int,
    val productId: Int,
    val productName: String,
    val productType: ProductType,
    val count: Long,
): CollectionObject


