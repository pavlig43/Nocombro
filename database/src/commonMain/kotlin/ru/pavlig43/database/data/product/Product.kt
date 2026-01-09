package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.database.data.files.OwnerType

const val PRODUCT_TABLE_NAME = "product"

@Entity(
    tableName = PRODUCT_TABLE_NAME,
)
data class Product(

    val type: ProductType,

    @ColumnInfo("display_name")
    val displayName: String,

//    @ColumnInfo("unit")
//    val unit: ProductUnit,

    @ColumnInfo("created_at")
    val createdAt: LocalDate,

    val comment: String = "",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
) : GenericItem





