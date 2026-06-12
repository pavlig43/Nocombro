package ru.pavlig43.database.data.batch.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.batch.BATCH_COST_PRICE_TABLE_NAME
import ru.pavlig43.database.data.batch.BatchCostPriceEntity

@Dao
interface BatchCostDao {
    @Upsert
    suspend fun upsert(entities: List<BatchCostPriceEntity>)

    @Query("SELECT * FROM $BATCH_COST_PRICE_TABLE_NAME WHERE batch_id in (:ids)")
    suspend fun getBatchesCostPriceByIds(ids: List<Int>): List<BatchCostPriceEntity>

    @Query("SELECT * FROM $BATCH_COST_PRICE_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getBySyncId(syncId: String): BatchCostPriceEntity?

    @Query("SELECT * FROM $BATCH_COST_PRICE_TABLE_NAME")
    suspend fun getAll(): List<BatchCostPriceEntity>
}
