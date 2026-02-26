package ru.pavlig43.database.data.storage

import ru.pavlig43.database.data.batch.MovementType

data class StorageProduct(
    val productId: Int,
    val productName: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,

    val batches: List<StorageBatch>
)
data class StorageBatch(
    val batchId: Int,
    val batchName: String,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
)
