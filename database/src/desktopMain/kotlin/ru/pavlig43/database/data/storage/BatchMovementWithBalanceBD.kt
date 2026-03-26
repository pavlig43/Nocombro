package ru.pavlig43.database.data.storage

import kotlinx.datetime.LocalDateTime

data class BatchMovementWithBalanceBD(
    val movementDate: LocalDateTime,
    val balanceBeforeStart: Long,
    val incoming: Long,
    val outgoing: Long,
    val balanceOnEnd: Long,
    val transactionId: Int,
)

data class BatchMovementWithBalanceInfoBD(
    val batchId: Int,
    val productName: String,
    val movements: List<BatchMovementWithBalanceBD>
)
