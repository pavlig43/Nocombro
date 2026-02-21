package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.BatchOut
import ru.pavlig43.database.data.batch.BatchWithBalanceOut
import ru.pavlig43.database.data.batch.MovementType

@Dao
interface BatchDao {

    @Update
    suspend fun updateBatch(batch: BatchBD)

    @Insert
    suspend fun createBatch(batchBD: BatchBD): Long

    /**
     * Получает партии по productId с вычисленным остатком.
     *
     * Использует combine для объединения партий и их движений,
     * затем вычисляет баланс (сумма INCOMING - сумма OUTGOING).
     *
     * @param productId Идентификатор продукта для фильтрации партий
     * @return Flow со списком партий с вычисленным остатком
     */
    fun observeBatchesByProductId(productId: Int): Flow<List<BatchWithBalanceOut>> {
        val batchesFlow = observeBatchOutByProductId(productId)
        val movementsFlow = observeAllMovements()

        return combine(batchesFlow, movementsFlow) { batches, allMovements ->
            batches.map { batchOut ->
                val batchMovements = allMovements.filter { it.batchId == batchOut.batch.id }
                val balance = calculateBalance(batchMovements)

                BatchWithBalanceOut(
                    batch = batchOut.batch,
                    product = batchOut.product,
                    declaration = batchOut.declaration,
                    balance = balance
                )
            }
        }
    }

    /**
     * Вычисляет остаток по списку движений.
     *
     * @param movements Список движений партии
     * @return Баланс (положительный = наличие, отрицательный = перерасход)
     */
    private fun calculateBalance(movements: List<BatchMovement>): Int {
        val incoming = movements.filter { it.movementType == MovementType.INCOMING }.sumOf { it.count }
        val outgoing = movements.filter { it.movementType == MovementType.OUTGOING }.sumOf { it.count }
        return incoming - outgoing
    }

    /**
     * Получает партии по productId со связанными сущностями.
     *
     * @param productId Идентификатор продукта
     * @return Flow со списком партий
     */
    @Query("SELECT * FROM batch WHERE product_id = :productId")
    @Transaction
    abstract fun observeBatchOutByProductId(productId: Int): Flow<List<BatchOut>>

    /**
     * Получает все движения для вычисления остатков.
     *
     * @return Flow со всеми движениями партий
     */
    @Query("SELECT * FROM batch_movement")
    abstract fun observeAllMovements(): Flow<List<BatchMovement>>
}
