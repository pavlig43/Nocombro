package ru.pavlig43.storage.internal.model

import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.tablecore.model.IMultiLineTableUi

internal data class StorageProductUi(
    override val composeId: Int,
    val productId: Int,
    val productName: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val batches: List<StorageBatchUi>
) : IMultiLineTableUi

internal fun StorageProduct.toUi(): StorageProductUi {
    return StorageProductUi(
        composeId = productId,
        productId = productId,
        productName = productName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd,
        batches = batches.map { it.toUi() }
    )
}
