package ru.pavlig43.database.data.storage

import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.transact.StockOperationReason
import kotlinx.datetime.LocalDateTime

data class BatchMovementWithBalanceBD(
    val movementDate: LocalDateTime,
    val balanceBeforeStart: Long,
    val incoming: Long,
    val outgoing: Long,
    val balanceOnEnd: Long,
    val transactionId: Int,
    val storageLocation: StorageLocation = StorageLocation.MAIN,
    val reason: StockOperationReason? = null,
)

data class BatchMovementWithBalanceInfoBD(
    val batchId: Int,
    val productName: String,
    val movements: List<BatchMovementWithBalanceBD>
)
