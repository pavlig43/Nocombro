package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.pavlig43.core.emptyDate
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.ingredient.IngredientBD

@Dao
abstract class CompositionDao {

    @Transaction
    @Query("SELECT * FROM composition WHERE parent_id = :parentId")
    internal abstract suspend fun getComposition(parentId: Int): List<InternalComposition>

    suspend fun getCompositionOut(parentId: Int): List<CompositionOut> {
        return getComposition(parentId).map(InternalComposition::toCompositionOut)
    }

    suspend fun getIngredientsFromComposition(
        productId: Int,
        transactionId: Int,
        countPf: Int
    ): List<IngredientBD> {
        return getComposition(productId).map {
            it.toIngredients(transactionId, countPf)
        }
    }

    @Upsert
    abstract suspend fun upsertComposition(composition: List<CompositionIn>)

    @Query("DELETE FROM composition WHERE id in(:ids)")
    abstract suspend fun deleteCompositions(ids: List<Int>)

    @Query(
        """
        SELECT * FROM composition
    """
    )
    internal abstract fun observeOnComposition(): Flow<List<CompositionIn>>


    fun observeProductWithoutComposition(observeOnAllProduct: () -> Flow<List<Product>>): Flow<List<NotificationDTO>> {
        return combine(
            observeOnAllProduct(),
            observeOnComposition()
        ) { products, compositions ->
            val productWithComposition = compositions.map { it.parentId }.toSet()
            products.filter {
                it.type in arrayOf(
                    ProductType.FOOD_BASE,
                    ProductType.FOOD_PF
                ) && it.id !in productWithComposition
            }
                .map { NotificationDTO(it.id, "В продукте ${it.displayName} нет состава") }
        }

    }
}

internal data class InternalComposition(
    @Embedded
    val composition: CompositionIn,
    @Relation(
        entity = Product::class,
        parentColumn = "product_id",
        entityColumn = "id"
    )
    val product: Product,

    )

private fun InternalComposition.toCompositionOut(): CompositionOut {
    return CompositionOut(
        id = composition.id,
        productId = product.id,
        productName = product.displayName,
        productType = product.type,
        count = composition.count
    )
}

private fun InternalComposition.toIngredients(transactionId: Int, countPf: Int): IngredientBD {
    return IngredientBD(
        transactionId = transactionId,
        batchId = 0,
        dateBorn = emptyDate,
        movementId = 0,
        count = composition.count * countPf,
        productType = product.type,
        productId = product.id,
        productName = product.displayName,
        vendorName = "",
        id = 0
    )
}
