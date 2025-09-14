package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.DeclarationIn
import ru.pavlig43.core.data.DeclarationOut
import ru.pavlig43.database.data.document.Document


@Entity(
    tableName = "product_declaration",
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
data class ProductDeclaration(

    @ColumnInfo("product_id")
    override val parentId: Int,

    @ColumnInfo("document_id")
    override val documentId: Int,

    @ColumnInfo("is_actual")
    override val isActual: Boolean,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
):DeclarationIn


data class ProductDeclarationOutWithDocumentName(
    override val id: Int,
    override val parentId: Int,
    override val documentId: Int,
    override val isActual: Boolean,
    override val displayName: String,
) : DeclarationOut


