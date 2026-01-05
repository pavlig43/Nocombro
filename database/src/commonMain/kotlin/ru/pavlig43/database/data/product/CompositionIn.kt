package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.CollectionObject


@Entity(
    tableName = "composition",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE,
        )
    ]

)
data class CompositionIn(
    @PrimaryKey(autoGenerate = true)
    override val id: Int,

    @ColumnInfo("product_id")
    val productId: Int,

    val count: Int,
) : CollectionObject

data class CompositionOut(
    override val id: Int,
    val productId: Int,
    val productName: String,
    val productType: ProductType,
    val count: Int,
): CollectionObject


