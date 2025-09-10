package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.pavlig43.core.UTC
import ru.pavlig43.core.data.Item

const val PRODUCT_TABLE_NAME = "product"
@Entity(
    tableName = PRODUCT_TABLE_NAME,
)
data class Product(

    override val type: ProductType,

    @ColumnInfo("display_name")
    override val displayName: String,

    @ColumnInfo("created_at")
    override val createdAt: Long,

    override val comment:String ="",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
) : Item





