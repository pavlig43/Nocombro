package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.data.CollectionObject
import ru.pavlig43.core.getCurrentLocalDate
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

    @ColumnInfo("product_id")
     val productId: Int,

    @ColumnInfo("declaration_id")
     val declarationId: Int,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
): CollectionObject


data class ProductDeclarationOut(
    override val id: Int,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val isActual: Boolean,
) : CollectionObject


