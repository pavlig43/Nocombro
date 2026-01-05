package ru.pavlig43.database.data.transaction.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.transaction.Transaction

@Dao
interface ProductTransactionDao {


@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun create(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("DELETE FROM product_transaction  WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: Set<Int>)

    @Query("SELECT * from product_transaction WHERE id = :id")
    suspend fun getTransaction(id: Int): Transaction

    @Query("""
    SELECT * FROM product_transaction
    ORDER BY created_at DESC
""")
    fun observeOnProductTransactions(): Flow<List<Transaction>>


    //TODO сделать проверку транзакций
    suspend fun isCanSave(transaction: Transaction): Result<Unit> {
        return Result.success(Unit)
    }

}

