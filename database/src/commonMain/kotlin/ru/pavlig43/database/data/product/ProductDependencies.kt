package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey


@Entity(
    tableName = "product_component",
    primaryKeys = ["parent_id", "child_id"],
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["parent_id"],
        onDelete = ForeignKey.CASCADE
    ),
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["child_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProductDependencies(

    @ColumnInfo("parent_id")
    val parentId: Int,

    @ColumnInfo("child_id")
    val childId: Int,

    val percent: Double,
)
