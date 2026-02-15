package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.core.mapValues
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut

@Dao
abstract class ProductDeclarationDao {

    @Upsert
    abstract suspend fun upsertProductDeclarations(declaration: List<ProductDeclarationIn>)

    @Query("DELETE FROM product_declaration WHERE id in(:ids)")
    abstract suspend fun deleteDeclarations(ids: List<Int>)

    @Transaction
    @Query(
        """
        SELECT * FROM product_declaration
        WHERE product_id = :productId
    """
    )
    internal abstract suspend fun getProductDeclaration(productId: Int): List<InternalProductDeclaration>

    suspend fun getProductDeclarationOut(productId: Int): List<ProductDeclarationOut> {
        return getProductDeclaration(productId).map(InternalProductDeclaration::toProductDeclarationOut)
    }



    @Transaction
    @Query(
        """
        SELECT * FROM product_declaration
    """
    )
    internal abstract fun observeOnProductDeclaration(): Flow<List<InternalProductDeclaration>>



    @Query("SELECT declaration_id FROM product_declaration WHERE product_id = :productId")
    abstract suspend fun getDeclarationIdsByProductId(productId: Int): List<Int>
    @Query("SELECT * FROM product_declaration WHERE product_id = :productId")
    internal abstract fun observeOnProductDeclarationByProductId(productId: Int): Flow<List<InternalProductDeclaration>>

    fun observeOnProductDeclarationOutByProductId(productId: Int): Flow<List<ProductDeclarationOut>> {
        return observeOnProductDeclarationByProductId(productId).mapValues(
            InternalProductDeclaration::toProductDeclarationOut
        )
    }

    fun observeOnProductDeclarationOut(): Flow<List<ProductDeclarationOut>> {
        return observeOnProductDeclaration().mapValues(InternalProductDeclaration::toProductDeclarationOut)
    }


    fun observeOnProductDeclarationNotification(
        observeOnAllProduct: () -> Flow<List<Product>>
    ): Flow<List<NotificationDTO>> {
        return combine(
            observeOnAllProduct(),
            observeOnProductDeclaration()
        ) { products, declarations ->
            // 1. ID продуктов С декларациями
            val productsWithDeclarations = declarations.map { it.product.id }.toSet()

            // 2. Продукты БЕЗ деклараций
            val withoutDeclarations = products
                .filter { it.id !in productsWithDeclarations }
                .map { NotificationDTO(it.id, "В продукте ${it.displayName} нет декларации") }

            // 3. Продукты, где ВСЕ декларации просрочены
            val allExpired = declarations
                .groupBy { it.product.id }
                .filterValues { group ->
                    group.all { it.declaration.bestBefore <= getCurrentLocalDate() }
                }
                .mapNotNull { (productId, _) ->
                    declarations.find { it.product.id == productId }?.let {
                        NotificationDTO(
                            it.product.id,
                            "В продукте ${it.product.displayName} нет свежей декларации"
                        )
                    }
                }

            // 4. ОБЪЕДИНЯЕМ ВСЕ
            withoutDeclarations + allExpired
        }

    }


}

internal data class InternalProductDeclaration(
    @Embedded
    val productDeclaration: ProductDeclarationIn,
    @Relation(
        parentColumn = "declaration_id",
        entityColumn = "id"
    )
    val declaration: Declaration,

    @Relation(
        parentColumn = "product_id",
        entityColumn = "id"
    )
    val product: Product
)

private fun InternalProductDeclaration.toProductDeclarationOut(): ProductDeclarationOut {
    return ProductDeclarationOut(
        id = productDeclaration.id,
        productId = productDeclaration.productId,
        declarationId = productDeclaration.declarationId,
        declarationName = declaration.displayName,
        vendorName = declaration.vendorName,
        isActual = declaration.bestBefore > getCurrentLocalDate(),
    )
}