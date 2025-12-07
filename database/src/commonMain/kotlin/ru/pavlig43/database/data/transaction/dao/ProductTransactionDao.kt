package ru.pavlig43.database.data.transaction.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.common.IsCanUpsertResult
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.database.data.transaction.TransactionType

@Dao
interface ProductTransactionDao {


@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun create(transaction: ProductTransaction): Long

    @Update
    suspend fun updateTransaction(transaction: ProductTransaction)

    @Query("DELETE FROM product_transaction  WHERE id IN (:ids)")
    suspend fun deleteProductTransactionsByIds(ids: List<Int>)

    @Query("SELECT * from product_transaction WHERE id = :id")
    suspend fun getProductTransaction(id: Int): ProductTransaction

    @Query("""
    SELECT * FROM product_transaction
    WHERE transaction_type IN (:types)
    AND (
        comment LIKE '%' || :searchText || '%'
        OR :searchText = ''
    )
    ORDER BY created_at DESC
""")
    fun observeOnProductTransactions(
        searchText: String,
        types: List<TransactionType>): Flow<List<ProductTransaction>>


    //TODO сделать проверку транзакций
    suspend fun isCanSave(transaction: ProductTransaction): IsCanUpsertResult{
        return IsCanUpsertResult.Ok()
    }

}

