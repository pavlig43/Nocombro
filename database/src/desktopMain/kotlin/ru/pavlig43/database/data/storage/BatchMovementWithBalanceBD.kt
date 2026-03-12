package ru.pavlig43.database.data.storage

import kotlinx.datetime.LocalDateTime

data class BatchMovementWithBalanceBD(
    val movementDate: LocalDateTime,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val transactionId: Int,
)

data class BatchMovementWithBalanceInfoBD(
    val batchId: Int,
    val productName: String,
    val movements: List<BatchMovementWithBalanceBD>
)
