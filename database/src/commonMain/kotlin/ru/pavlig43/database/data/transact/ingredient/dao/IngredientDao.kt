package ru.pavlig43.database.data.transact.ingredient.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.BatchOut
import ru.pavlig43.database.data.transact.ingredient.IngredientBD

@Dao
abstract class IngredientDao {

    @Transaction
    @Query("""
        SELECT * FROM batch_movement
        WHERE transaction_id = :transactionId
            AND movement_type = 'OUTGOING'
        ORDER BY id DESC    
    """)
    internal abstract suspend fun getIngredients(transactionId: Int): List<InternalIngredient>

    suspend fun getByTransactionId(transactionId: Int): List<IngredientBD> {
        return getIngredients(transactionId).map { it.toIngredientBD() }
    }

}

internal data class InternalIngredient(
    @Embedded
    val movement: BatchMovement,
    @Relation(
        entity = BatchBD::class,
        parentColumn = "batch_id",
        entityColumn = "id"
    )
    val batchOut: BatchOut
)

private fun InternalIngredient.toIngredientBD(): IngredientBD {
    val product = batchOut.product
    val declaration = batchOut.declaration

    return IngredientBD(
        transactionId = movement.transactionId,
        batchId = movement.batchId,
        dateBorn = batchOut.batch.dateBorn,
        movementId = movement.id,
        count = movement.count,
        productId = product.id,
        productName = product.displayName,
        vendorName = declaration.vendorName,
        id = movement.id
    )
}
