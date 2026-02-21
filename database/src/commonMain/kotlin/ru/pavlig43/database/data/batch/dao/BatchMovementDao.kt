package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.BatchOut

/**
 * DAO для работы с движениями партий (batch movements).
 *
 * Предоставляет методы для CRUD операций над движениями партий,
 * а также для получения движений с загруженными связанными сущностями.
 */
@Dao
abstract class BatchMovementDao {

    /**
     * Создаёт новое движение партии.
     *
     * @param batchMovement Движение для создания
     * @return Идентификатор созданной записи
     */
    @Insert
    abstract suspend fun createMovement(batchMovement: BatchMovement): Long

    /**
     * Обновляет существующее движение партии.
     *
     * @param movement Движение для обновления
     */
    @Update
    abstract suspend fun updateMovement(movement: BatchMovement)

    /**
     * Вставляет список движений с заменой при конфликте.
     *
     * @param movements Список движений для вставки
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMovements(movements: List<BatchMovement>)

    /**
     * Вставляет или обновляет движение партии.
     *
     * @param movement Движение для сохранения
     */
    @Upsert
    abstract suspend fun upsertMovement(movement: BatchMovement)

    /**
     * Удаляет все движения для указанной транзакции.
     *
     * @param transactionId Идентификатор транзакции
     */
    @Query("DELETE FROM batch_movement WHERE transaction_id = :transactionId")
    abstract suspend fun deleteByTransactionId(transactionId: Int)

    /**
     * Получает движения для указанной транзакции с загруженными партиями.
     *
     * Использует @Relation для автоматической загрузки связанной партии (BatchBD).
     *
     * @param transactionId Идентификатор транзакции
     * @return Список движений с загруженными партиями
     */
    @Query("SELECT * FROM batch_movement WHERE transaction_id = :transactionId")
    @Transaction
    abstract suspend fun getByTransactionId(transactionId: Int): List<MovementOut>

    /**
     * Удаляет движения с указанными идентификаторами.
     *
     * @param ids Список идентификаторов для удаления
     */
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
}

/**
 * Выходное DTO движения партии с загруженной связанной партией.
 *
 * Использует @Relation для автоматической загрузки партии (BatchBD).
 *
 * @property movement Движение партии
 * @property batchOut Связанная партия с продуктом
 */
data class MovementOut(
    @Embedded
    val movement: BatchMovement,
    @Relation(
        entity = BatchBD::class,
        parentColumn = "batch_id",
        entityColumn = "id"
    )
    val batchOut: BatchOut
)
