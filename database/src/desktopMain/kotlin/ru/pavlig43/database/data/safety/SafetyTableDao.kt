package ru.pavlig43.database.data.safety

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.mapParallel
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.dao.MovementOut
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.SafetyStock


@Dao
abstract class SafetyTableDao {

    @Transaction
    @Query("SELECT * FROM batch_movement")
    internal abstract fun observeOnAllMovements(): Flow<List<MovementOut>>

    internal fun observeOnCountProductOnStorage(): Flow<List<StorageProductNow>> {
        return observeOnAllMovements().map { movements ->
            movements
                .groupBy { it.batchOut.product }
                .values
                .mapParallel(Dispatchers.Default) { movementOuts ->
                    val product = movementOuts.first().batchOut.product

                    val count = movementOuts.sumOf { movementOut ->
                        when (movementOut.movement.movementType) {
                            MovementType.INCOMING -> movementOut.movement.count
                            MovementType.OUTGOING -> -movementOut.movement.count
                        }
                    }

                    StorageProductNow(
                        productId = product.id,
                        count = count
                    )
                }
        }
    }

    @Query("SELECT * FROM safety_stock")
    internal abstract fun observeOnSafetyStock(): Flow<List<SafetyStock>>

    @Transaction
    @Query("SELECT * FROM safety_stock")
    internal abstract fun observeOnSafetyStockWithProductAndDeclaration(): Flow<List<SafetyStockWithProductAndDeclaration>>

    fun observeOnSafetyTableItems(): Flow<List<SafetyTableItem>> {
        return combine(
            observeOnCountProductOnStorage(),
            observeOnSafetyStockWithProductAndDeclaration()
        ) { storageList, safetyWithProductAndDeclarationList ->
            val countMap = storageList.associateBy { it.productId }

            safetyWithProductAndDeclarationList.mapNotNull { safetyWithProductAndDeclaration ->
                val safety = safetyWithProductAndDeclaration.safetyStock
                val product = safetyWithProductAndDeclaration.product
                val declaration = safetyWithProductAndDeclaration.declarations.firstOrNull()
                val currentCount = countMap[product.id]?.count ?: 0

                if (currentCount < safety.reorderPoint) {
                    SafetyTableItem(
                        productId = product.id,
                        productName = product.displayName,
                        vendorName = declaration?.vendorName.orEmpty(),
                        count = currentCount,
                        reorderPoint = safety.reorderPoint,
                        orderQuantity = safety.orderQuantity
                    )
                } else {
                    null
                }
            }.sortedBy { it.productName }
        }
    }
}

internal data class StorageProductNow(
    val productId: Int,
    val count: Long
)

internal data class SafetyStockWithProduct(
    @Embedded
    val safetyStock: SafetyStock,

    @Relation(parentColumn = "product_id", entityColumn = "id")
    val product: Product
)

internal data class SafetyStockWithProductAndDeclaration(
    @Embedded
    val safetyStock: SafetyStock,

    @Relation(entity = Product::class, parentColumn = "product_id", entityColumn = "id")
    val product: Product,

    @Relation(
        entity = Declaration::class,
        parentColumn = "product_id",
        entityColumn = "id",
        associateBy = Junction(
            value = ProductDeclarationIn::class,
            parentColumn = "product_id",
            entityColumn = "declaration_id"
        )
    )
    val declarations: List<Declaration>
)
