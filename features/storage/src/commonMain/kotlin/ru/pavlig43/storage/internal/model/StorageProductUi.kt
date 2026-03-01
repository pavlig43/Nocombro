package ru.pavlig43.storage.internal.model

import ru.pavlig43.database.data.storage.StorageProduct

data class StorageProductUi(
    val productId: Int,
    val itemId: Int,
    val name: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val isProduct: Boolean,
    val expanded: Boolean = false
)


