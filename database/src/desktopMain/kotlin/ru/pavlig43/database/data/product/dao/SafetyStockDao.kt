package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.product.SAFETY_STOCK_TABLE_NAME
import ru.pavlig43.database.data.product.SafetyStock

@Dao
interface SafetyStockDao {
    @Query("SELECT * FROM safety_stock WHERE product_id = :productId")
    suspend fun getByProductId(productId: Int): SafetyStock?

    @Query("SELECT * FROM safety_stock WHERE sync_id = :syncId")
    suspend fun getBySyncId(syncId: String): SafetyStock?

    @Query("SELECT * FROM $SAFETY_STOCK_TABLE_NAME")
    suspend fun getAll(): List<SafetyStock>

    @Upsert
    suspend fun upsert(safetyStock: SafetyStock)

    @Delete
    suspend fun delete(safetyStock: SafetyStock)

}
