package ru.pavlig43.storage.internal.model

internal data class StorageProductUi(
    val productId: Int,
    val productName: String,
    val itemId: Int,
    val itemName: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val isProduct: Boolean,
    val isExpanded: Boolean = false,
    val batchId: Int = itemId,  // Для партий batchId = itemId, для продуктов itemId = productId
)

