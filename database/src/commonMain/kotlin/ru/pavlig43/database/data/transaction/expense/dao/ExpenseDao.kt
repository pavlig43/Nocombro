package ru.pavlig43.database.data.transaction.expense.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.transaction.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.transaction.expense.ExpenseBD

@Dao
interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(expenses: List<ExpenseBD>)

    @Query("DELETE FROM $EXPENSE_TABLE_NAME WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Int>)



    /**
     * Получить расходы по ID транзакции (только привязанные) - синхронно
     */
    @Query("SELECT * FROM $EXPENSE_TABLE_NAME WHERE transaction_id = :transactionId ORDER BY expense_date_time ASC")
    suspend fun getByTransactionId(transactionId: Int): List<ExpenseBD>

    /**
     * Получить ВСЕ расходы (включая непривязанные к транзакции)
     */
    @Query("SELECT * FROM $EXPENSE_TABLE_NAME ORDER BY expense_date_time DESC")
    fun observeAll(): Flow<List<ExpenseBD>>

    /**
     * Получить все расходы (включая непривязанные) - синхронно
     */
    @Query("SELECT * FROM $EXPENSE_TABLE_NAME ORDER BY expense_date_time DESC")
    suspend fun getAll(): List<ExpenseBD>

}
