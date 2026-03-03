package ru.pavlig43.database.data.batch

import kotlinx.datetime.LocalDateTime

data class BatchMovementWithBalance(
    val movementId: Int,
    val batchId: Int,
    val batchName: String,
    val productName: String,
    val movementDate: LocalDateTime,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val transactionId: Int
)
