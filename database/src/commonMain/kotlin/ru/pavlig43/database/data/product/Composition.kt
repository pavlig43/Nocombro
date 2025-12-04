package ru.pavlig43.database.data.product

import androidx.room.*
import ru.pavlig43.core.data.CollectionObject

@Entity(
    tableName = "product_composition",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["product_id"],
        onDelete = ForeignKey.CASCADE
    )
    ]
)
data class ProductComposition(
    @PrimaryKey(autoGenerate = true)
     val id: Int,

    @ColumnInfo("product_id")
    val productId: Int,

    val name: String
)
data class ProductCompositionIn(
    val compositionForSave: ProductComposition,
    val ingredients: List<ProductIngredientIn>,

):CollectionObject{
    @Ignore
    override val id: Int = compositionForSave.id
}

data class ProductCompositionOut(
    @Embedded
    val composition: ProductComposition,

    @Relation(
        parentColumn = "id",
        entityColumn = "composition_id",
        entity = ProductIngredientIn::class
    )
    val ingredients: List<ProductIngredientOut>,

) : CollectionObject{
    @Ignore
    override val id: Int = composition.id
}


//////////////////
@Entity(
    tableName = "product_ingredient",
    foreignKeys = [ForeignKey(
        entity = Product::class,
        parentColumns = ["id"],
        childColumns = ["ingredient_id"],
        onDelete = ForeignKey.CASCADE
    ),
        ForeignKey(
            entity = ProductComposition::class,
            parentColumns = ["id"],
            childColumns = ["composition_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProductIngredientIn(

    @ColumnInfo("composition_id")
    val compositionId: Int,

    @ColumnInfo("ingredient_id")
    val ingredientId: Int,

    @ColumnInfo("count_grams")
    val countGrams: Int,

    @PrimaryKey(autoGenerate = true)
    val id: Int,
)


data class ProductIngredientOut(
    @Embedded
    val ingredient: ProductIngredientIn,
    @Relation(
        entity = Product::class,
        parentColumn =  "ingredient_id",
        entityColumn = "id"
    )
    val product: Product,

    )






