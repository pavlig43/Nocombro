package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun create(product: Product): Long

    @Update
    suspend fun updateProduct(product: Product)

    @Query("DELETE FROM product WHERE id IN (:ids)")
    suspend fun deleteProductsByIds(ids: List<Int>)

    @Query("SELECT * from product WHERE id = :id")
    suspend fun getProduct(id: Int): Product

    @Query("""
    SELECT * FROM $PRODUCT_TABLE_NAME
    WHERE type IN (:types)
    AND (
        display_name LIKE '%' || :searchText || '%' 
        OR comment LIKE '%' || :searchText || '%'
        OR :searchText = ''
    )
    ORDER BY created_at DESC
""")
    fun observeOnProducts(
        searchText: String,
        types: List<ProductType>): Flow<List<Product>>


    @RawQuery(observedEntities = [Product::class])
    fun observeOnItems(query: RoomRawQuery):Flow<List<Product>>

    @Query(
        """
        SELECT CASE
            WHEN (SELECT display_name FROM product WHERE id =:id) =:name THEN TRUE
            ELSE NOT EXISTS (SELECT 1 FROM product WHERE display_name = :name AND id != :id)
        END
    """
    )
    suspend fun isNameAllowed(id: Int, name: String): Boolean



}
