package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.product.Product

@Dao
interface ProductDao {

    @Query("SELECT * FROM product WHERE id=:id")
    suspend fun getBaseComponent(id:Int): Product

    @Query("SELECT *  FROM product")
    fun getBaseComponents():Flow<List<Product>>

    @Update
    suspend fun updateBaseComponent(baseComponent: Product)

    @Delete
    suspend fun deleteBaseComponent(baseComponent: Product)

    @Insert
    suspend fun createBaseComponent(baseComponent: Product)
}