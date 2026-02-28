package ru.pavlig43.immutable.internal.component.items.storage

import ru.pavlig43.database.data.storage.StorageBatch
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class StorageProductUi(
    override val composeId: Int,
    val productId: Int,
    val productName: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val batches: List<StorageBatchUi>,
    val expanded: Boolean = false
) : IMultiLineTableUi

internal fun StorageProduct.toUi(expanded: Boolean = false): StorageProductUi {
    return StorageProductUi(
        composeId = productId,
        productId = productId,
        productName = productName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd,
        batches = batches.map { it.toUi() },
        expanded = expanded
    )
}
data class StorageBatchUi(
    val batchId: Int,
    val batchName: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val composeId: Int = batchId
)

internal fun StorageBatch.toUi(): StorageBatchUi {
    return StorageBatchUi(
        batchId = batchId,
        batchName = batchName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd
    )
}