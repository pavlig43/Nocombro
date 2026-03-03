package ru.pavlig43.immutable.internal.component.items.batchMovement

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.tablecore.model.IMultiLineTableUi

internal data class BatchMovementTableUi(
    val movementId: Int = 0,
    val batchId: Int = 0,
    val batchName: String = "",
    val productName: String = "",
    val movementDate: LocalDateTime,
    val balanceBeforeStart: Int = 0,
    val incoming: Int = 0,
    val outgoing: Int = 0,
    val balanceOnEnd: Int = 0,
    val transactionId: Int = 0,
    override val composeId: Int = 0,
) : IMultiLineTableUi
