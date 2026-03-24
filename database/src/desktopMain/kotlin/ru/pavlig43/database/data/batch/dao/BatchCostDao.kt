package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.batch.BatchCostPriceEntity

@Dao
interface BatchCostDao {
    @Upsert
    suspend fun upsert(entities: List<BatchCostPriceEntity>)

    @Query("SELECT * FROM batch_cost_price WHERE batch_id in (:ids)")
    suspend fun getBatchesCostPriceByIds(ids: List<Int>): List<BatchCostPriceEntity>

}