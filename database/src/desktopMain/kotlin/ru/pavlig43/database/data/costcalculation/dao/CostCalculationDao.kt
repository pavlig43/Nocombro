package ru.pavlig43.database.data.costcalculation.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType

/**
 * DAO для расчёта себестоимости и отчётов по продажам.
 */
@Dao
interface CostCalculationDao {

    /**
     * Получить транзакции указанного типа за период.
     */
    @Query("""
        SELECT * FROM transact
        WHERE transaction_type = :type
        AND created_at BETWEEN :start AND :end
        ORDER BY created_at DESC
    """)
    suspend fun getTransactionsByTypeAndPeriod(
        type: TransactionType,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Transact>

    /**
     * Получить цену покупки по ID партии.
     * Ищет запись в таблице buy, связанную с первым INCOMING движением партии.
     */
    @Query("""
        SELECT b.price FROM buy b
        INNER JOIN batch_movement bm ON b.movement_id = bm.id
        WHERE bm.batch_id = :batchId
        LIMIT 1
    """)
    suspend fun getPriceByBatchId(batchId: Int): Int?

    /**
     * Получить входящее движение для транзакции.
     */
    @Query("""
        SELECT * FROM batch_movement
        WHERE transaction_id = :transactionId
        AND movement_type = 'INCOMING'
        LIMIT 1
    """)
    suspend fun getIncomingMovement(transactionId: Int): BatchMovement?

    /**
     * Получить ID OPZS транзакции, которая создала партию.
     */
    @Query("""
        SELECT t.id FROM transact t
        INNER JOIN batch_movement bm ON t.id = bm.transaction_id
        WHERE bm.batch_id = :batchId
        AND t.transaction_type = 'OPZS'
        LIMIT 1
    """)
    suspend fun getOpzsTransactionForBatch(batchId: Int): Int?

    /**
     * Получить общие расходы (не привязанные к транзакции) за период.
     */
    @Query("""
        SELECT * FROM expense
        WHERE transaction_id IS NULL
        AND expense_date_time BETWEEN :start AND :end
    """)
    suspend fun getGeneralExpensesForPeriod(
        start: LocalDateTime,
        end: LocalDateTime
    ): List<ExpenseBD>

    /**
     * Получить сумму стоимости материалов для всех OPZS за период.
     * Суммирует (цена * количество) для всех ингредиентов OPZS.
     */
    @Query("""
        SELECT COALESCE(SUM(b.price * bm.count), 0) FROM batch_movement bm
        INNER JOIN buy b ON bm.id = b.movement_id
        INNER JOIN transact t ON bm.transaction_id = t.id
        WHERE t.transaction_type = 'OPZS'
        AND t.created_at BETWEEN :start AND :end
        AND bm.movement_type = 'OUTGOING'
    """)
    suspend fun getTotalMaterialsCostForPeriod(
        start: LocalDateTime,
        end: LocalDateTime
    ): Int

    /**
     * Получить прямые расходы по транзакции.
     */
    @Query("""
        SELECT * FROM expense
        WHERE transaction_id = :transactionId
        ORDER BY expense_date_time ASC
    """)
    suspend fun getDirectExpenses(transactionId: Int): List<ExpenseBD>
}
