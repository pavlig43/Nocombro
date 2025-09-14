package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.product.ProductComposition
import ru.pavlig43.database.data.product.ProductCompositionIn
import ru.pavlig43.database.data.product.ProductCompositionOut
import ru.pavlig43.database.data.product.ProductIngredientIn

@Dao
interface CompositionDao {


    @Query("SELECT * FROM product_composition WHERE product_id = :productId")
    suspend fun getCompositions(productId: Int): List<ProductCompositionOut>


    @Insert
    suspend fun internalCreateComposition(composition: ProductComposition): Long

    @Update
    suspend fun internalUpdateComposition(compositions: List<ProductComposition>)

    @Query("DELETE FROM product_composition WHERE id in(:ids)")
    suspend fun deleteCompositions(ids: List<Int>)

    @Transaction
    suspend fun upsertCompositions(compositions: List<ProductCompositionIn>) {
        val newCompositions = compositions.filter { it.id == 0 }
        val newIndexesMap =
            newCompositions.associateBy { internalCreateComposition(it.compositionForSave).toInt() }
        newIndexesMap.forEach { (id, composition) ->
            val ingredients = composition.ingredients .map {
                it.copy(compositionId = id)
            }
            upsertProductIngredient(ingredients)
        }
        val existingCompositions = compositions.filter { it.id != 0 }
        internalUpdateComposition(existingCompositions.map { it.compositionForSave })

        existingCompositions.forEach { composition ->
            deleteIngredientsFromComposition(composition.id)
            val ingredients = composition.ingredients .map {
                it.copy(compositionId = composition.id)
            }

            upsertProductIngredient(ingredients)

        }

    }

    ////////////////////////////////

    @Upsert
    suspend fun upsertProductIngredient(ingredients: List<ProductIngredientIn>)


    @Query("DELETE FROM product_ingredient WHERE composition_id = :compositionId")
    suspend fun deleteIngredientsFromComposition(compositionId: Int)


    @Query("""
    SELECT 
        p.id as id,
        p.display_name || ' @ ' || pc.name as displayName
    FROM product p
    JOIN product_composition pc ON p.id = pc.product_id
    WHERE p.type != 'BASE'
    AND EXISTS (
        SELECT 1 
        FROM product_ingredient pi 
        WHERE pi.composition_id = pc.id
        GROUP BY pi.composition_id
        HAVING SUM(pi.count_grams) != 1000
    )
""")
    fun observeOnProductWhereIngredientsNotEquals1000gram(): Flow<List<NotificationDTO>>

    @Query("""
    SELECT 
        p.id as id, 
        p.display_name as displayName 
    FROM product p
    WHERE p.type != 'BASE' 
    AND NOT EXISTS (
        SELECT 1 
        FROM product_composition pc 
        WHERE pc.product_id = p.id
    )
""")
    fun observeProductWithoutComposition():Flow<List<NotificationDTO>>
}