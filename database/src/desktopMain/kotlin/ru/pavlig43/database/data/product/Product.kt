package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

const val PRODUCT_TABLE_NAME = "product"

@Entity(
    tableName = PRODUCT_TABLE_NAME,
    indices = [Index(value = ["sync_id"], unique = true)]
)
data class Product(

    val type: ProductType,

    @ColumnInfo("display_name")
    val displayName: String,

    @ColumnInfo("second_name", defaultValue = "")
    val secondName: String = "",


//    @ColumnInfo("unit")
//    val unit: ProductUnit,

    @ColumnInfo("created_at")
    val createdAt: LocalDate,

    val comment: String = "",

    @ColumnInfo("price_for_sale")
    val priceForSale: Long = 0,

    @ColumnInfo("shelf_life_days")
    val shelfLifeDays: Int = 0,

    @ColumnInfo("rec_nds")
    val recNds: Int = 0,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : SingleItem





