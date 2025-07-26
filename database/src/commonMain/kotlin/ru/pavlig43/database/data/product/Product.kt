package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.UTC
import ru.pavlig43.core.data.Item
import ru.pavlig43.database.data.document.Document

@Entity(
    tableName = "product",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Product(

    override val type: ProductType,

    @ColumnInfo("declaration_id")
    val declarationId:Int?,

    @ColumnInfo("display_name")
    override val displayName: String,

    @ColumnInfo("created_at")
    override val createdAt: UTC,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
) : Item





