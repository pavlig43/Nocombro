package ru.pavlig43.storage.api.component.batchMovement

import kotlinx.datetime.LocalDateTime

data class BatchMovementTableUi(
    val movementDate: LocalDateTime,
    val balanceBeforeStart: Int,
    val incoming: Int,
    val outgoing: Int,
    val balanceOnEnd: Int,
    val transactionId: Int,
)

internal data class BatchMovementInfo(
    val productName: String,
    val batchName: String,
    val movements: List<BatchMovementTableUi>
)
