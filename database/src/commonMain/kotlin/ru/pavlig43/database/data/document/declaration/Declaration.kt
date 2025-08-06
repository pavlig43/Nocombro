package ru.pavlig43.database.data.document.declaration

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Relation
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.product.Product


@Entity(
    tableName = "declaration",
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
            onDelete = ForeignKey.SET_NULL
        )
    ]

)
data class Declaration(

    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("document_id")
    val documentId: Int,

    @ColumnInfo("is_actual")
    val isActual: Boolean,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)

data class DeclarationWithDocuments(
    @Embedded
    val declaration: Declaration,
    @Relation(
        entity = Document::class,
        parentColumn = "document_id",
        entityColumn = "id"
    )
    val documents: Document
)
