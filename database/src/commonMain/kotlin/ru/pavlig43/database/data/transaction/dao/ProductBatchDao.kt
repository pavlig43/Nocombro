package ru.pavlig43.database.data.transaction.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Upsert
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transaction.ProductBatchTransactionBDIn
import ru.pavlig43.database.data.transaction.ProductBatchTransactionBDOut

@Dao
abstract class ProductBatchDao {

    @Query("""
        DELETE FROM product_batch_transaction
         WHERE id IN(:ids)
    """)
    abstract suspend fun deleteBatchesRows(ids: List<Int>)

    @Upsert
    abstract suspend fun upsert(batches: List<ProductBatchTransactionBDIn>)
    @Query(
        """
        SELECT * FROM product_batch_transaction
        WHERE transaction_id=:transactionId
    """
    )
    internal abstract suspend fun internalBatchesRow(transactionId: Int): List<InternalProductBatchTransactionRow>

    suspend fun getProductBatchesRow(transactionId: Int) =
        internalBatchesRow(transactionId).map { it.toBDOut() }


}

internal data class InternalProductBatchTransactionRow(

    @Embedded
    val productBatchTransaction: ProductBatchTransactionBDIn,

    @Relation(parentColumn = "product_id", entityColumn = "id")
    val product: Product,

    @Relation(parentColumn = "declaration_id", entityColumn = "id")
    val declaration: Declaration,
)

private fun InternalProductBatchTransactionRow.toBDOut(): ProductBatchTransactionBDOut {
    return ProductBatchTransactionBDOut(
        productId = product.id,
        productName = product.displayName,
        declarationId = declaration.id,
        declarationName = declaration.displayName,
        vendorName = declaration.vendorName,
        dateBorn = productBatchTransaction.dateBorn,
        batch = productBatchTransaction.batch,
        id = productBatchTransaction.id
    )
}