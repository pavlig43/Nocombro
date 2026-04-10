package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.batch.BATCH_TABLE_NAME
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement

@Dao
interface BatchDao {

    @Update
    suspend fun updateBatch(batch: BatchBD)

    @Insert
    suspend fun createBatch(batchBD: BatchBD): Long

    @Query("SELECT * FROM $BATCH_TABLE_NAME WHERE id = :id")
    suspend fun getBatch(id: Int): BatchBD

    @Query("SELECT * FROM $BATCH_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getBatchBySyncId(syncId: String): BatchBD?


    /**
     * Получает все движения для вычисления остатков.
     *
     * @return Flow со всеми движениями партий
     */
    @Query("SELECT * FROM batch_movement")
    fun observeAllMovements(): Flow<List<BatchMovement>>
}
