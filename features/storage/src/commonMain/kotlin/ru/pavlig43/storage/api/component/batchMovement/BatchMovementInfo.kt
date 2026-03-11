package ru.pavlig43.storage.api.component.batchMovement

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat

data class BatchMovementTableUi(
    val movementDate: LocalDateTime,
    val balanceBeforeStart: DecimalData,
    val incoming: DecimalData,
    val outgoing: DecimalData,
    val balanceOnEnd: DecimalData,
    val transactionId: Int,
)

internal data class BatchMovementInfo(
    val productName: String,
    val batchName: String,
    val movements: List<BatchMovementTableUi>
)
