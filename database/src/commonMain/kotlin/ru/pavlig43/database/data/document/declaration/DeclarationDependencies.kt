package ru.pavlig43.database.data.document.declaration

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.product.Product


@Entity(
    tableName = "declaration_dependencies",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["product_id"],
        onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
        entity = Document::class,
        parentColumns = ["id"],
        childColumns = ["declaration_id"],
        onDelete = ForeignKey.SET_NULL
    )
    ]

)
data class DeclarationDependencies(

    @ColumnInfo("product_id")
    val productId: Int,

    @ColumnInfo("declaration_id")
    val declarationId: Int,

    @ColumnInfo("is_actual")
    val isActual: Boolean,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
)
