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

const val PRODUCT_SPECIFICATION_TABLE_NAME = "product_specification"

@Entity(
    tableName = PRODUCT_SPECIFICATION_TABLE_NAME,
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["product_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["product_id"], unique = true),
        Index(value = ["sync_id"], unique = true),
    ]
)
data class ProductSpecification(
    @ColumnInfo("product_id")
    val productId: Int,

    val description: String = "",

    val dosage: String = "",

    val composition: String = "",

    @ColumnInfo("shelf_life_text")
    val shelfLifeText: String = "",

    @ColumnInfo("storage_conditions")
    val storageConditions: String = "",

    @ColumnInfo("appearance")
    val appearance: String = "",

    @ColumnInfo("color")
    val color: String = "",

    @ColumnInfo("smell")
    val smell: String = "",

    @ColumnInfo("taste")
    val taste: String = "",

    @ColumnInfo("physical_chemical_indicators")
    val physicalChemicalIndicators: String = "",

    @ColumnInfo("microbiological_indicators")
    val microbiologicalIndicators: String = "",

    @ColumnInfo("toxic_elements")
    val toxicElements: String = "",

    @ColumnInfo("allergens")
    val allergens: String = "",

    @ColumnInfo("gmo_info")
    val gmoInfo: String = "",

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,

    @ColumnInfo("sync_id")
    val syncId: String = defaultSyncId(),

    @ColumnInfo("updated_at")
    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    @ColumnInfo("deleted_at")
    val deletedAt: LocalDateTime? = null,
) : SingleItem
