package ru.pavlig43.database.data.safety

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.mapParallel
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.dao.MovementOut
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

                    // Берём vendorName из первой декларации
                    val vendorName = movementOuts.first().batchOut.declaration.vendorName

                    // Вычисляем текущий остаток
                    val count = movementOuts.sumOf { movementOut ->
                        when (movementOut.movement.movementType) {
                            MovementType.INCOMING -> movementOut.movement.count
                            MovementType.OUTGOING -> -movementOut.movement.count
                        }
                    }

                    StorageProductNow(
                        productId = product.id,
                        productName = product.displayName,
                        vendorName = vendorName,
                        count = count
                    )
                }
                .sortedBy { it.productName }
        }
    }
    @Query("SELECT * FROM safety_stock")
    internal abstract fun observeOnSafetyStock():Flow<List<SafetyStock>>

    fun observeOnSafetyTableItems(): Flow<List<SafetyTableItem>> {
        return combine(
            observeOnCountProductOnStorage(),
            observeOnSafetyStock()
        ) { storageList, safetyList ->
            // Создаём Map для быстрого поиска safety_stock по productId
            val safetyMap = safetyList.associateBy { it.productId }

            storageList
                .mapNotNull { storage ->
                    val safety = safetyMap[storage.productId]
                    if (safety != null && storage.count < safety.reorderPoint) {
                        SafetyTableItem(
                            productId = storage.productId,
                            productName = storage.productName,
                            vendorName = storage.vendorName,
                            count = storage.count,
                            reorderPoint = safety.reorderPoint,
                            orderQuantity = safety.orderQuantity
                        )
                    } else {
                        null
                    }
                }
                .sortedBy { it.productName }
        }
    }
}

internal data class StorageProductNow(
    val productId: Int,
    val productName: String,
    val vendorName: String,
    val count: Int
)