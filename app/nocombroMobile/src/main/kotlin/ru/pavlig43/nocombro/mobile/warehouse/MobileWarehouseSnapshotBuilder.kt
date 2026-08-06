package ru.pavlig43.nocombro.mobile.warehouse

import java.lang.Math.addExact
import java.lang.Math.subtractExact
import kotlinx.datetime.LocalDateTime

internal data class RemoteWarehouseProductRow(
    val syncId: String,
    val type: String,
    val displayName: String,
    val deletedAt: LocalDateTime? = null,
)

internal data class RemoteWarehouseBatchRow(
    val syncId: String,
    val productSyncId: String,
    val deletedAt: LocalDateTime? = null,
)

internal data class RemoteWarehouseMovementRow(
    val syncId: String,
    val batchSyncId: String,
    val movementType: String,
    val count: Long,
    val transactionSyncId: String,
    val storageLocation: String?,
    val deletedAt: LocalDateTime? = null,
)

internal data class RemoteWarehouseTransactionRow(
    val syncId: String,
    val transactionType: String,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

internal fun buildMobileWarehouseSnapshot(
    snapshotAt: LocalDateTime,
    products: List<RemoteWarehouseProductRow>,
    batches: List<RemoteWarehouseBatchRow>,
    movements: List<RemoteWarehouseMovementRow>,
    transactions: List<RemoteWarehouseTransactionRow>,
): MobileWarehouseSnapshot {
    val activeProducts = products.filter { it.deletedAt == null }
    activeProducts.forEach { row ->
        require(row.type in PRODUCT_TYPES) {
            "Unknown product type: sync_id=${row.syncId}, value=${row.type}"
        }
    }
    val productById = activeProducts.associateUniqueBy("product", RemoteWarehouseProductRow::syncId)

    val activeBatches = batches.filter { it.deletedAt == null }
    val batchById = activeBatches.associateUniqueBy("batch", RemoteWarehouseBatchRow::syncId)
    activeBatches.forEach { row ->
        require(row.productSyncId in productById) {
            "Broken active batch-product relation: batch_sync_id=${row.syncId}"
        }
    }

    val activeTransactions = transactions.filter { it.deletedAt == null }
    activeTransactions.forEach { row ->
        require(row.transactionType in TRANSACTION_TYPES) {
            "Unknown transaction type: sync_id=${row.syncId}, value=${row.transactionType}"
        }
    }
    val transactionById = activeTransactions.associateUniqueBy(
        "transact",
        RemoteWarehouseTransactionRow::syncId,
    )

    val balances = activeProducts.associate { product ->
        product.syncId to longArrayOf(0L, 0L)
    }.toMutableMap()

    movements.filter { it.deletedAt == null }.forEach { movement ->
        require(movement.movementType in MOVEMENT_TYPES) {
            "Unknown movement type: sync_id=${movement.syncId}, value=${movement.movementType}"
        }
        val location = when (movement.storageLocation) {
            null, MobileWarehouseLocation.MAIN.name -> MobileWarehouseLocation.MAIN
            MobileWarehouseLocation.EXPERIMENTAL.name -> MobileWarehouseLocation.EXPERIMENTAL
            else -> error(
                "Unknown storage location: sync_id=${movement.syncId}, " +
                    "value=${movement.storageLocation}"
            )
        }
        val batch = requireNotNull(batchById[movement.batchSyncId]) {
            "Broken active movement-batch relation: movement_sync_id=${movement.syncId}"
        }
        val transaction = requireNotNull(transactionById[movement.transactionSyncId]) {
            "Broken active movement-transaction relation: movement_sync_id=${movement.syncId}"
        }
        if (transaction.createdAt > snapshotAt) return@forEach

        val productBalances = balances.getValue(batch.productSyncId)
        val locationIndex = if (location == MobileWarehouseLocation.MAIN) 0 else 1
        productBalances[locationIndex] = when (movement.movementType) {
            MOVEMENT_INCOMING -> addExact(productBalances[locationIndex], movement.count)
            MOVEMENT_OUTGOING -> subtractExact(productBalances[locationIndex], movement.count)
            else -> error("Validated movement type became invalid")
        }
    }

    return MobileWarehouseSnapshot(
        updatedAt = snapshotAt,
        products = activeProducts.map { product ->
            val productBalances = balances.getValue(product.syncId)
            MobileWarehouseProduct(
                productSyncId = product.syncId,
                displayName = product.displayName,
                mainBalance = productBalances[0],
                experimentalBalance = productBalances[1],
            )
        },
    )
}

private inline fun <T> List<T>.associateUniqueBy(
    tableName: String,
    keySelector: (T) -> String,
): Map<String, T> {
    val result = linkedMapOf<String, T>()
    forEach { row ->
        val key = keySelector(row)
        require(result.put(key, row) == null) {
            "Duplicate active $tableName sync_id=$key"
        }
    }
    return result
}

private const val MOVEMENT_INCOMING = "INCOMING"
private const val MOVEMENT_OUTGOING = "OUTGOING"
private val MOVEMENT_TYPES = setOf(MOVEMENT_INCOMING, MOVEMENT_OUTGOING)
private val PRODUCT_TYPES = setOf("FOOD_BASE", "FOOD_PF", "PACK")
private val TRANSACTION_TYPES = setOf(
    "BUY",
    "SALE",
    "OPZS",
    "WRITE_OFF",
    "STORAGE_TRANSFER",
    "INVENTORY",
)
