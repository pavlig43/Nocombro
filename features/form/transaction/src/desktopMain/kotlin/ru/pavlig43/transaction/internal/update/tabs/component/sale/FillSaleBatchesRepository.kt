package ru.pavlig43.transaction.internal.update.tabs.component.sale

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.transact.sale.SaleBDOut
import kotlin.math.min

/** Подбирает доступные партии для всех строк продажи. */
internal interface FillSaleBatchesRepository {
    suspend fun fill(
        transactionId: Int,
        sales: List<SaleBDOut>,
    ): Result<FillSaleBatchesResult>
}

/** Читает остатки партий из Room и передаёт их чистой FIFO-функции. */
internal class DefaultFillSaleBatchesRepository(
    db: NocombroDatabase,
) : FillSaleBatchesRepository {
    private val batchMovementDao = db.batchMovementDao

    override suspend fun fill(
        transactionId: Int,
        sales: List<SaleBDOut>,
    ): Result<FillSaleBatchesResult> = runCatching {
        val movements = batchMovementDao.getActiveMainMovementsForProducts(
            productIds = sales.map(SaleBDOut::productId).distinct(),
            excludedTransactionId = transactionId,
        )
        val availableBatches = movements
            .groupBy { it.movement.batchId }
            .values
            .mapNotNull { batchMovements ->
                val balance = batchMovements.sumOf { movementOut ->
                    when (movementOut.movement.movementType) {
                        MovementType.INCOMING -> movementOut.movement.count
                        MovementType.OUTGOING -> -movementOut.movement.count
                    }
                }
                if (balance <= 0L) return@mapNotNull null

                val batch = batchMovements.first().batchOut
                AvailableSaleBatch(
                    batchId = batch.batch.id,
                    batchSyncId = batch.batch.syncId,
                    productId = batch.product.id,
                    vendorName = batch.declaration.vendorName,
                    dateBorn = batch.batch.dateBorn,
                    balance = balance,
                )
            }

        allocateSalesByBatches(
            sales = sales,
            availableBatches = availableBatches,
        )
    }
}

/** Итог расчёта, который можно применить к форме лишь целиком. */
internal sealed interface FillSaleBatchesResult {
    data class Ready(val sales: List<SaleBDOut>) : FillSaleBatchesResult
    data class Deficit(val deficits: List<SaleBatchDeficit>) : FillSaleBatchesResult
}

internal data class SaleBatchDeficit(
    val productId: Int,
    val productName: String,
    val missingCount: Long,
)

internal data class AvailableSaleBatch(
    val batchId: Int,
    val batchSyncId: String,
    val productId: Int,
    val vendorName: String,
    val dateBorn: LocalDate,
    val balance: Long,
)

/**
 * Обрабатывает строки в исходном порядке и делит общий остаток партии между ними.
 * При любом дефиците готовые строки не возвращаются.
 */
internal fun allocateSalesByBatches(
    sales: List<SaleBDOut>,
    availableBatches: List<AvailableSaleBatch>,
): FillSaleBatchesResult {
    val batchesByProduct = availableBatches
        .groupBy(AvailableSaleBatch::productId)
        .mapValues { (_, batches) ->
            batches.sortedWith(compareBy(AvailableSaleBatch::dateBorn, AvailableSaleBatch::batchId))
        }
    val remainingByBatch = availableBatches.associate { it.batchId to it.balance }.toMutableMap()
    val allocatedSales = mutableListOf<SaleBDOut>()
    val deficitsByProduct = linkedMapOf<Int, SaleBatchDeficit>()

    sales.forEach { sale ->
        var remaining = sale.count
        var isFirstPart = true
        for (batch in batchesByProduct[sale.productId].orEmpty()) {
            if (remaining <= 0L) break
            val batchRemaining = remainingByBatch[batch.batchId] ?: 0L
            if (batchRemaining <= 0L) continue

            val allocatedCount = min(batchRemaining, remaining)
            val identity = if (isFirstPart) {
                SaleIdentity(
                    id = sale.id,
                    syncId = sale.syncId,
                    movementId = sale.movementId,
                    movementSyncId = sale.movementSyncId,
                )
            } else {
                SaleIdentity(
                    id = 0,
                    syncId = defaultSyncId(),
                    movementId = 0,
                    movementSyncId = defaultSyncId(),
                )
            }
            allocatedSales += sale.copy(
                count = allocatedCount,
                batchId = batch.batchId,
                batchSyncId = batch.batchSyncId,
                vendorName = batch.vendorName,
                dateBorn = batch.dateBorn,
                id = identity.id,
                syncId = identity.syncId,
                movementId = identity.movementId,
                movementSyncId = identity.movementSyncId,
            )
            remainingByBatch[batch.batchId] = batchRemaining - allocatedCount
            remaining -= allocatedCount
            isFirstPart = false
        }

        if (remaining > 0L) {
            val oldDeficit = deficitsByProduct[sale.productId]
            deficitsByProduct[sale.productId] = SaleBatchDeficit(
                productId = sale.productId,
                productName = oldDeficit?.productName ?: sale.productName,
                missingCount = (oldDeficit?.missingCount ?: 0L) + remaining,
            )
        }
    }

    return if (deficitsByProduct.isEmpty()) {
        FillSaleBatchesResult.Ready(allocatedSales)
    } else {
        FillSaleBatchesResult.Deficit(deficitsByProduct.values.toList())
    }
}

private data class SaleIdentity(
    val id: Int,
    val syncId: String,
    val movementId: Int,
    val movementSyncId: String,
)
