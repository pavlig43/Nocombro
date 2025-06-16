package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.UTC
import ru.pavlig43.database.data.common.data.Item
import ru.pavlig43.database.data.document.Document

@Entity(
    tableName = "product",
)
data class Product(

    override val type: ProductType,

    @ColumnInfo("display_name")
    override val displayName: String,

    override val createdAt: UTC,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
) : Item





