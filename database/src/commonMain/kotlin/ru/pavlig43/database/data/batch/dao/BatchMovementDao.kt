package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.pavlig43.database.data.batch.BatchMovement

@Dao
interface BatchMovementDao {

    @Insert
    suspend fun insertMovement(batchMovement: BatchMovement): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMovements(movements: List<BatchMovement>)

    @Query("DELETE FROM batch_movement WHERE transaction_id = :transactionId")
    suspend fun deleteByTransactionId(transactionId: Int)

    @Query("SELECT * FROM batch_movement WHERE transaction_id = :transactionId")
    suspend fun getByTransactionId(transactionId: Int): List<BatchMovement>
}
