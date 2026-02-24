package ru.pavlig43.database.data.batch

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
    val movementType: MovementType,
    val count: Int,
    val transactionId: Int,
)
