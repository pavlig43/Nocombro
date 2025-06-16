package ru.pavlig43.database.data.specification

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.product.Product


@Entity(
    tableName = "specification",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["product_id"],
        onDelete = ForeignKey.CASCADE
    ),
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["document_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Specification(

    val name: String,

    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("document_id")
    val documentId: Int,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)