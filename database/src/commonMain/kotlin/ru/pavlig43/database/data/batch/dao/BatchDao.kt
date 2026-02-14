package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import ru.pavlig43.database.data.batch.BatchBD

@Dao
interface BatchDao {

    @Update
    suspend fun updateBatch(batch: BatchBD)

    @Insert
    suspend fun createBatch(batchBD: BatchBD): Long

}
