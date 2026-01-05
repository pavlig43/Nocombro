package ru.pavlig43.database.data.product.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.DateThreshold
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.database.data.common.NotificationDTO
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut

@Dao
abstract class ProductDeclarationDao {

    @Upsert
    abstract suspend fun upsertProductDeclarations(declaration: List<ProductDeclarationIn>)

    @Query("DELETE FROM product_declaration WHERE id in(:ids)")
    abstract suspend fun deleteDeclarations(ids: List<Int>)

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

    @Query(
        """
        SELECT * FROM product_declaration
    """
    )
    internal abstract fun observeOnProductDeclaration(): Flow<List<InternalProductDeclaration>>

    fun observeOnProductWithoutActualDeclaration(observeOnAllProduct:Flow<List<Product>>): Flow<List<NotificationDTO>> {
        return observeOnProductDeclaration().map { lst ->
            lst.groupBy { it.product.id }
                .filterValues { group ->
                    group.all { it.declaration.bestBefore <= getCurrentLocalDate() }
                }
                .keys.mapNotNull { productId ->
                    lst.find { it.product.id == productId }?.let {
                        NotificationDTO(it.product.id, it.product.displayName)
                    }
                }
        }
    }
    fun observeOnProductDeclarationNotification(
        observeOnAllProduct:()->Flow<List<Product>>): Flow<List<NotificationDTO>> {
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

            // 3. Продукты где ВСЕ декларации просрочены
            val allExpired = declarations
                .groupBy { it.product.id }
                .filterValues { group ->
                    group.all { it.declaration.bestBefore <= getCurrentLocalDate() }
                }
                .mapNotNull { (productId, _) ->
                    declarations.find { it.product.id == productId }?.let {
                        NotificationDTO(it.product.id, "В продукте ${it.product.displayName} нет свежей декларации")
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
        declarationId = productDeclaration.declarationId,
        declarationName = declaration.displayName,
        vendorName = declaration.vendorName,
        isActual = declaration.bestBefore > getCurrentLocalDate(),
    )
}