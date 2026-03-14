package ru.pavlig43.storage.api.component.batchMovement

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData3

data class BatchMovementTableUi(
    val movementDate: LocalDateTime,
    val balanceBeforeStart: DecimalData3,
    val incoming: DecimalData3,
    val outgoing: DecimalData3,
    val balanceOnEnd: DecimalData3,
    val transactionId: Int,
)

internal data class BatchMovementInfo(
    val productName: String,
    val batchName: String,
    val movements: List<BatchMovementTableUi>
)
