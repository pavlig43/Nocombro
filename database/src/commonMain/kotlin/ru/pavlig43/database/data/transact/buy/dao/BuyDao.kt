package ru.pavlig43.database.data.transact.buy.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.BuyBDOut

@Dao
abstract class BuyDao {

    @Transaction
    @Query("SELECT * FROM $BUY_TABLE_NAME WHERE transaction_id = :transactionId ORDER BY id DESC")
    internal abstract suspend fun getBuysWithRelations(transactionId: Int): List<InternalBuy>

    suspend fun getBuysWithDetails(transactionId: Int): List<BuyBDOut> {
        return getBuysWithRelations(transactionId).map(InternalBuy::toBuyBDOut)
    }

    @Upsert
    abstract suspend fun upsertBuyBd(buy: BuyBDIn)

    @Query("DELETE FROM $BUY_TABLE_NAME WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Int>)

    @Query("SELECT movement_id FROM $BUY_TABLE_NAME WHERE id IN (:ids)")
    abstract suspend fun getMovementIdsByBuyIds(ids: List<Int>): List<Int>
}

internal data class InternalBuy(
    @Embedded
    val buy: BuyBDIn,
    @Relation(
        entity = Transact::class,
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val transaction: Transact,
    @Relation(
        entity = BatchMovement::class,
        parentColumn = "movement_id",
        entityColumn = "id",
    )
    val movementOut: MovementOut

)


private fun InternalBuy.toBuyBDOut(): BuyBDOut {
    val batchOut = movementOut.batchOut

    return BuyBDOut(
        transactionId = transaction.id,
        productId = batchOut.product.id,
        declarationId = batchOut.declaration.id,
        productName = batchOut.product.displayName,
        dateBorn = batchOut.batch.dateBorn,
        count = movementOut.movement.count,
        batchId = movementOut.movement.batchId,
        declarationName = batchOut.declaration.displayName,
        vendorName = batchOut.declaration.vendorName,
        price = buy.price,
        comment = buy.comment,
        id = buy.id,
        movementId = movementOut.movement.id
    )
}
