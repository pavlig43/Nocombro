package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOutWithDocumentName

@Dao
interface ProductDeclarationDao {

    @Upsert
    suspend fun upsertProductDeclarations(declaration: List<ProductDeclaration>)

    @Query("DELETE FROM product_declaration WHERE id in(:ids)")
    suspend fun deleteDeclarations(ids:List<Int>)

    @Query(
        """
    SELECT pd.id, 
           pd.product_id AS parentId,
           d.id AS documentId, 
           pd.is_actual AS isActual,
           d.display_name AS displayName
    FROM product_declaration pd
    JOIN document d ON pd.document_id = d.id
    WHERE pd.product_id = :productId
    """
    )
    suspend fun getProductDeclarationWithDocumentName(productId: Int): List<ProductDeclarationOutWithDocumentName>
}