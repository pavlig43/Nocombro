package ru.pavlig43.database.data.transaction.buy.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.database.data.transaction.buy.BUY_TABLE_NAME
import ru.pavlig43.database.data.transaction.buy.BuyBDIn
import ru.pavlig43.database.data.transaction.buy.BuyBDOut

@Dao
abstract class BuyDao {

    @Query("SELECT * FROM $BUY_TABLE_NAME ORDER BY id DESC")
    internal abstract suspend fun getAllBuysWithRelations(): List<InternalBuy>

    suspend fun getAllBuysWithDetails(): List<BuyBDOut> {
        return getAllBuysWithRelations().map(InternalBuy::toBuyBDOut)
    }

    @Query("SELECT * FROM $BUY_TABLE_NAME WHERE transaction_id = :transactionId ORDER BY id DESC")
    abstract fun observeByTransactionId(transactionId: Int): Flow<List<BuyBDIn>>

    @Upsert
    abstract suspend fun upsert(buy: BuyBDIn)

    @Query("DELETE FROM $BUY_TABLE_NAME WHERE id IN (:ids)")
    abstract suspend fun deleteByIds(ids: List<Int>)
}

internal data class InternalBuy(
    @Embedded
    val buy: BuyBDIn,
    @Relation(
        entity = Transaction::class,
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val transaction: Transaction,
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
    val declaration: Declaration,
)

private fun InternalBuy.toBuyBDOut(): BuyBDOut {
    return BuyBDOut(
        productName = product.displayName,
        dateBorn = buy.dateBorn,
        count = buy.count,
        declarationName = declaration.displayName,
        vendorName = declaration.vendorName,
        price = buy.price,
        comment = buy.comment,
        id = buy.id
    )
}
