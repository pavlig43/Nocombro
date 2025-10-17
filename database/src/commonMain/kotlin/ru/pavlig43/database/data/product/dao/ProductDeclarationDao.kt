package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOutWithNameAndVendor
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME

@Dao
interface ProductDeclarationDao {

    @Upsert
    suspend fun upsertProductDeclarations(declaration: List<ProductDeclaration>)

    @Query("DELETE FROM product_declaration WHERE id in(:ids)")
    suspend fun deleteDeclarations(ids:List<Int>)


    @Query(
        """
    SELECT pd.id,
           pd.product_id AS productId,
           d.id AS declarationId,
           d.display_name AS declarationName,
           v.display_name AS vendorName,
           d.best_before AS bestBefore
    FROM product_declaration pd
    JOIN $DECLARATIONS_TABLE_NAME d ON pd.declaration_id = d.id
    JOIN $VENDOR_TABLE_NAME v ON d.vendor_id = v.id
    WHERE pd.product_id = :productId AND d.best_before < datetime('now')
    """
    )
    suspend fun getProductDeclarationWithDocumentName(productId: Int): List<ProductDeclarationOutWithNameAndVendor>


    @Query("""
        SELECT p.id,p.display_name AS displayName
        FROM product_declaration pd
        JOIN $PRODUCT_TABLE_NAME p ON pd.product_id = p.id
        JOIN $DECLARATIONS_TABLE_NAME d ON pd.declaration_id = d.id
        WHERE d.best_before < strftime('%s', 'now') * 1000
    """)
    fun observeOnProductWithExpiredDeclaration():Flow<List<NotificationDTO>>
}