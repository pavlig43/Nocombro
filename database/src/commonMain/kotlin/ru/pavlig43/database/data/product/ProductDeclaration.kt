package ru.pavlig43.database.data.product

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import ru.pavlig43.core.data.GenericDeclarationIn
import ru.pavlig43.core.data.GenericDeclarationOut
import ru.pavlig43.database.data.declaration.DeclarationIn


@Entity(
    tableName = "product_declaration",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["product_id"],
        onDelete = ForeignKey.CASCADE
    ),
        ForeignKey(
            entity = DeclarationIn::class,
            parentColumns = ["id"],
            childColumns = ["declaration_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ]

)
data class ProductDeclaration(

    @ColumnInfo("product_id")
    override val productId: Int,

    @ColumnInfo("declaration_id")
    override val declarationId: Int,

    @PrimaryKey(autoGenerate = true)
    override val id: Int = 0,
):GenericDeclarationIn


data class ProductDeclarationOutWithNameAndVendor(
    override val id: Int,
    override val productId: Int,
    override val declarationId: Int,
    override val declarationName: String,
    override val vendorName: String,
    override val bestBefore: Long,
) : GenericDeclarationOut


