package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.Product

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun create(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM product WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: Set<Int>)

    @Query("SELECT * from product WHERE id = :id")
    suspend fun getProduct(id: Int): Product

    @Query("SELECT * from product WHERE sync_id = :syncId")
    suspend fun getProductBySyncId(syncId: String): Product?

    @Query("""
    SELECT * FROM $PRODUCT_TABLE_NAME
    ORDER BY created_at DESC
""")
    fun observeOnProducts(): Flow<List<Product>>


    suspend fun isCanSave(product: Product): Result<Unit> {
        return Result.success(Unit)

    }


}
