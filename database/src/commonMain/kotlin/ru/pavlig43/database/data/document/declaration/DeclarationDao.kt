package ru.pavlig43.database.data.document.declaration

import androidx.room.Dao
import androidx.room.Query

@Dao
interface DeclarationDao {
    @Query("SELECT * FROM declaration WHERE product_id =:productId")
    suspend fun getDeclarationByProductId(productId:Int):List<DeclarationWithDocuments>
}