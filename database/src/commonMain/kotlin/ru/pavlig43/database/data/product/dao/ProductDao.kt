package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType

@Dao
interface ProductDao {

    @Query("SELECT *  FROM product WHERE type in (:types) ORDER BY created_at DESC")
    fun observeProductsByProductType(types:List<ProductType>):Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: Product):Long

    @Query("DELETE FROM product WHERE id IN (:ids)")
    suspend fun deleteProducts(ids: List<Int>)

    @Update
    suspend fun updateProduct(product: Product):Unit

    @Query("SELECT * from product WHERE id = :id")
    suspend fun getDocument(id: Int): Product

    @Query("SELECT * from product ORDER BY created_at ASC")
    fun observeAllProducts(): Flow<List<Product>>

    @Query("SELECT * from product WHERE type IN (:types) ORDER BY created_at DESC")
    fun observeProductsByTypes(types: List<ProductType>): Flow<List<Product>>

//    @Insert(onConflict = OnConflictStrategy.ABORT)
//    suspend fun insertDocumentPaths(filePaths:List<DocumentFilePath>)

//    @Query("DELETE  FROM document_file_path WHERE id in(:deletedIds)")
//    suspend fun deleteFilePaths(deletedIds:List<Int>)

//    @Query("SELECT * FROM document_file_path WHERE document_id = :documentId")
//    suspend fun getFilePaths(documentId: Int):List<DocumentFilePath>

    @Transaction
    suspend fun updateProduct(new: Product, old: Product){
        updateProduct(new)

    }

//    @Transaction
//    suspend fun insertDocumentWithWithFiles(documentWithFiles: DocumentWithFiles){
//        val documentId = insertDocument(documentWithFiles.document).toInt()
//        insertDocumentPaths(documentWithFiles.files.map { it.copy(documentId = documentId) })
//
//    }
    @Query("""
        SELECT CASE
            WHEN (SELECT display_name FROM product WHERE id =:id) THEN TRUE
            ELSE NOT EXISTS (SELECT 1 FROM product WHERE display_name = :name AND id != :id)
        END
    """)
    suspend fun isNameChangeAllowed(id: Int,name: String):Boolean

    @Query("SELECT COUNT(*) > 0 FROM product WHERE display_name =:name")
    suspend fun isNameExist(name: String):Boolean

}
