package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.declaration.Declaration


@Entity(
    tableName = "product_declaration",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["product_id"],
        onDelete = ForeignKey.CASCADE
    ),
        ForeignKey(
            entity = Declaration::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]

)
data class ProductDeclarationIn(

    @ColumnInfo("product_id", index = true)
     val productId: Int,

    @ColumnInfo("declaration_id", index = true)
     val declarationId: Int,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
): CollectionObject


data class ProductDeclarationOut(
    override val id: Int,
    val productId: Int,
    val displayName: String,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val isActual: Boolean,
) : CollectionObject


