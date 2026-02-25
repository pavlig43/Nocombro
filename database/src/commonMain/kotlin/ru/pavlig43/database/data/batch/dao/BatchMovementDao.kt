package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.mapParallel
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.BatchOut
import ru.pavlig43.database.data.batch.BatchWithBalanceOut
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.StorageBatch
import ru.pavlig43.database.data.batch.StorageProduct
import ru.pavlig43.database.data.transact.Transact

/**
 * DAO для работы с движениями партий (batch movements).
 *
 * Предоставляет методы для CRUD операций над движениями партий,
 * а также для получения движений с загруженными связанными сущностями.
 */
@Dao
abstract class BatchMovementDao {


    @Insert
    abstract suspend fun createMovement(batchMovement: BatchMovement): Long


    @Upsert
    abstract suspend fun upsertMovements(movements: List<BatchMovement>)


    @Upsert
    abstract suspend fun upsertMovement(movement: BatchMovement)



    @Query("SELECT * FROM batch_movement WHERE transaction_id = :transactionId")
    @Transaction
    internal abstract suspend fun getByTransactionId(transactionId: Int): List<MovementOut>

    @Transaction
    @Query("SELECT * FROM batch_movement WHERE batch_id IN (SELECT id FROM batch WHERE product_id = :productId)")
    internal abstract fun observeMovementsByProductId(productId: Int): Flow<List<MovementOut>>

    @Transaction
    @Query("SELECT * FROM batch_movement ")
    internal abstract fun observeOnAllMovements(): Flow<List<MovementOut>>

    fun observeOnStorageProducts1(): Flow<List<String>> {
        return observeOnAllMovements().mapValues { "$it \n" }
    }
    fun observeOnStorageProducts(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<String>> {
        val a = observeOnAllMovements().map{ lst->
            lst.groupBy { it.batchOut.product.id }
                .values.mapParallel { movementOuts: List<MovementOut> ->
                    StorageBatch
                }
                }
        }



    fun observeBatchWithBalanceByProductId(productId: Int): Flow<List<BatchWithBalanceOut>> {
        return observeMovementsByProductId(productId).map { lst ->
            lst.groupBy { it.movement.batchId }.values.mapParallel { movements ->
                val balance = movements.fold(0) { acc, out ->
                    val movementType = out.movement.movementType
                    val count = out.movement.count
                    when (movementType) {
                        MovementType.INCOMING -> acc + count
                        MovementType.OUTGOING -> acc - count
                    }
                }
                if (balance > 0) {
                    val first = movements.first()
                    BatchWithBalanceOut(
                        batchId = first.movement.batchId,
                        balance = balance,
                        productName = first.batchOut.product.displayName,
                        vendorName = first.batchOut.declaration.vendorName,
                        dateBorn = first.batchOut.batch.dateBorn
                    )
                } else null

            }.filterNotNull()
        }
    }


    @Query("DELETE FROM batch_movement WHERE id in (:ids)")
    abstract suspend fun deleteByIds(ids: List<Int>)

    /**
     * Получает все движения для указанной партии.
     *
     * @param batchId Идентификатор партии
     * @return Список всех движений партии
     */
    @Query("SELECT * FROM batch_movement WHERE batch_id = :batchId")
    abstract fun observeMovementsByBatchId(batchId: Int): Flow<List<BatchMovement>>


    internal fun observeOnMovementInDateTimePeriod(
        start: LocalDateTime,
        end: LocalDateTime
    ): Flow<List<StorageProduct>>{
        return flowOf()
    }
}

/**
 * Выходное DTO движения партии с загруженной связанной партией.
 *
 * Использует @Relation для автоматической загрузки партии (BatchBD).
 *
 * @property movement Движение партии
 * @property batchOut Связанная партия с продуктом
 */
internal data class MovementOut(
    @Embedded
    val movement: BatchMovement,
    @Relation(
        entity = BatchBD::class,
        parentColumn = "batch_id",
        entityColumn = "id"
    )
    val batchOut: BatchOut,

    @Relation(
        entity = Transact::class,
        parentColumn = "transaction_id",
        entityColumn = "id"
    )
    val transaction: Transact
)
