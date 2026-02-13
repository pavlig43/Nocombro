package ru.pavlig43.database.data.transact.buy.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.BuyBDOut

@Dao
abstract class BuyDao {

    @Transaction
    @Query("SELECT * FROM $BUY_TABLE_NAME WHERE transaction_id = :transactionId ORDER BY id DESC")
    internal abstract suspend fun getAllBuysWithRelations(transactionId: Int): List<InternalBuy>

    suspend fun getAllBuysWithDetails(transactionId: Int): List<BuyBDOut> {
        return getAllBuysWithRelations(transactionId).map(InternalBuy::toBuyBDOut)
    }
    @Query("SELECT * FROM $BUY_TABLE_NAME WHERE id = :id")
    abstract suspend fun getById(id: Int): BuyBDIn?

    @Upsert
    abstract suspend fun upsertBuyBd(buys: List<BuyBDIn>)

    @Query("DELETE FROM $BUY_TABLE_NAME WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Int>)
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
        entity = BatchBD::class,
        parentColumn = "batch_id",
        entityColumn = "id"
    )
    val batchOut: BatchOut

)
internal data class BatchOut(
    @Embedded
    val batch: BatchBD,
    @Relation(
        entity = Product::class,
        parentColumn = "product_id",
        entityColumn = "id"
    )
    val product: Product,
    @Relation(
        entity = Declaration::class,
        parentColumn = "declaration_id",
        entityColumn = "id"
    )
    val declaration: Declaration
)

private fun InternalBuy.toBuyBDOut(): BuyBDOut {

    return BuyBDOut(
        transactionId = transaction.id,
        productId = batchOut.product.id,
        declarationId = batchOut.declaration.id,
        productName = batchOut.product.displayName,
        dateBorn = batchOut.batch.dateBorn,
        count = buy.count,
        batchId = batchOut.batch.id,
        declarationName = batchOut.declaration.displayName,
        vendorName = batchOut.declaration.vendorName,
        price = buy.price,
        comment = buy.comment,
        id = buy.id
    )
}
