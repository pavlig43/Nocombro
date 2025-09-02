package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.product.ProductFile

@Dao
interface ProductFilesDao {
    @Query("SELECT * FROM product_file WHERE product_id = :productId")
    suspend fun getFiles(productId: Int):List<ProductFile>

    @Upsert
    suspend fun upsertProductFiles(files:List<ProductFile>)

    @Query("DELETE FROM product_file WHERE id in(:ids)")
    suspend fun deleteFiles(ids:List<Int>)
}