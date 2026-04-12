package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.product.PRODUCT_SPECIFICATION_TABLE_NAME

@Dao
interface ProductSpecificationDao {
    @Query("SELECT * FROM $PRODUCT_SPECIFICATION_TABLE_NAME WHERE product_id = :productId")
    suspend fun getByProductId(productId: Int): ProductSpecification?

    @Query("SELECT * FROM $PRODUCT_SPECIFICATION_TABLE_NAME WHERE sync_id = :syncId")
    suspend fun getBySyncId(syncId: String): ProductSpecification?

    @Upsert
    suspend fun upsert(specification: ProductSpecification)
}
