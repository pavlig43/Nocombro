package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement

@Dao
abstract class BatchDao {

    @Update
    abstract suspend fun updateBatch(batch: BatchBD)

    @Insert
    abstract suspend fun createBatch(batchBD: BatchBD): Long


    /**
     * Получает все движения для вычисления остатков.
     *
     * @return Flow со всеми движениями партий
     */
    @Query("SELECT * FROM batch_movement")
    abstract fun observeAllMovements(): Flow<List<BatchMovement>>
}
