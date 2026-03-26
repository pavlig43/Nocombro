package ru.pavlig43.database.data.storage

data class StorageProduct(
    val productId: Int,
    val productName: String,
    val balanceBeforeStart: Long,
    val incoming: Long,
    val outgoing: Long,
    val balanceOnEnd: Long,

    val batches: List<StorageBatch>
)
data class StorageBatch(
    val batchId: Int,
    val batchName: String,
    val balanceBeforeStart: Long,
    val incoming: Long,
    val outgoing: Long,
    val balanceOnEnd: Long,
)
