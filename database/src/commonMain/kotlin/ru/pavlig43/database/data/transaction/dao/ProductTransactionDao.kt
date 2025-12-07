package ru.pavlig43.database.data.transaction.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.database.data.transaction.ProductTransactionIn
import ru.pavlig43.database.data.transaction.ProductTransactionOut
import ru.pavlig43.database.data.transaction.TransactionRow
import ru.pavlig43.database.data.transaction.TransactionRowOut

@Dao
interface ProductTransactionDao {

    //Get
    @Query(
        """
        SELECT 
        tr.id,
        tr.product_id AS productId,
        p.display_name AS productName,
        tr.declaration_id AS declarationId,
        d.display_name || ' ' || d.vendor_name AS declarationWithVendorName,
        tr.date_born AS dateBorn,
        tr.batch AS batch
        FROM transaction_row tr
        JOIN $PRODUCT_TABLE_NAME p ON tr.product_id = p.id
        JOIN $DECLARATIONS_TABLE_NAME d ON tr.declaration_id = d.id
        WHERE tr.transaction_id = :transactionId
    """
    )
    suspend fun internalGetTransactionRows(transactionId: Int): List<TransactionRowOut>

    @Query("SELECT * FROM product_transaction WHERE id = :transactionId")
    suspend fun internalGetTransaction(transactionId: Int): ProductTransaction

    @Transaction
    suspend fun getTransactionWithProducts(transactionId: Int): ProductTransactionOut {
        val transaction = internalGetTransaction(transactionId)
        val productRows = internalGetTransactionRows(transactionId)
        return ProductTransactionOut(
            transaction = transaction,
            productRows = productRows
        )
    }

    //Create
    @Query("UPDATE product_transaction SET is_completed = :completed WHERE id = :id")
    suspend fun changeCompletedStatus(id: Int, completed: Boolean)

    @Insert
    suspend fun internalCreateTransaction(transaction: ProductTransaction): Long


    @Upsert
    suspend fun internalUpsertTransactionRow(rows: List<TransactionRow>)

    @Transaction
    suspend fun createTransactionWithRow(transactionIn: ProductTransactionIn): Int {
        val transactionId = internalCreateTransaction(transactionIn.transactionForSave).toInt()
        internalUpsertTransactionRow(transactionIn.products)
        return transactionId
    }


    //Update

    @Update
    suspend fun internalUpdateTransaction(transaction: ProductTransaction)

    @Query("DELETE FROM transaction_row WHERE id IN (:ids)")
    suspend fun deleteTransactionRows(ids: List<Int>)

    @Transaction
    suspend fun updateTransaction(
        transactionIn: ProductTransactionIn,
        oldRows: List<TransactionRow>
    ) {
        internalUpdateTransaction(transactionIn.transactionForSave)

        val newRows = transactionIn.products
        val newById = newRows.associateBy { it.id }

        val oldById = oldRows.associateBy { it.id }
        val idsForDelete = oldById.keys - newById.keys
        deleteTransactionRows(idsForDelete.toList())
        val rowsForUpsert = newRows.filter { row ->
            val oldRow = oldById[row.id]
            oldRow == null || oldRow != row
        }
        internalUpsertTransactionRow(rowsForUpsert)


    }

}

