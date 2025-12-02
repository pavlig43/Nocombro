package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.GenericItem

const val PRODUCT_TABLE_NAME = "product"

@Entity(
    tableName = PRODUCT_TABLE_NAME,
)
data class Product(

    val type: ProductType,

    @ColumnInfo("display_name")
    override val displayName: String,

//    @ColumnInfo("unit")
//    val unit: ProductUnit,

    @ColumnInfo("created_at")
    val createdAt: Long,

    val comment: String = "",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
) : GenericItem





