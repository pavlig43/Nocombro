package ru.pavlig43.database.data.transact.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.transact.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.transact.Transact

@Dao
interface TransactionDao {


@Insert(onConflict = OnConflictStrategy.REPLACE)
suspend fun create(transaction: Transact): Long

    @Update
    suspend fun updateTransaction(transaction: Transact)

    @Query("DELETE FROM $TRANSACTION_TABLE_NAME WHERE id IN (:ids)")
    suspend fun deleteTransactionsByIds(ids: Set<Int>)

    @Query("SELECT * FROM  $TRANSACTION_TABLE_NAME WHERE id = :id")
    suspend fun getTransaction(id: Int): Transact

    @Query("""
    SELECT * FROM $TRANSACTION_TABLE_NAME
    ORDER BY created_at DESC
""")
    fun observeOnProductTransactions(): Flow<List<Transact>>


    //TODO сделать проверку транзакций
    suspend fun isCanSave(transaction: Transact): Result<Unit> {
        return Result.success(Unit)
    }

}

