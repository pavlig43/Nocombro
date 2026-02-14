package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import androidx.room.Upsert
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.BatchOut

@Dao
abstract class BatchMovementDao {

    @Insert
    abstract suspend fun createMovement(batchMovement: BatchMovement): Long

    @Update
    abstract suspend fun updateMovement(movement: BatchMovement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMovements(movements: List<BatchMovement>)

    @Upsert
    abstract suspend fun upsertMovement(movement: BatchMovement)

    @Query("DELETE FROM batch_movement WHERE transaction_id = :transactionId")
    abstract suspend fun deleteByTransactionId(transactionId: Int)

    @Query("SELECT * FROM batch_movement WHERE transaction_id = :transactionId")
    abstract suspend fun getByTransactionId(transactionId: Int): List<MovementOut>

    @Query("DELETE FROM batch_movement WHERE id in (:ids)")
    abstract suspend fun deleteByIds(ids:List<Int>)


}
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
