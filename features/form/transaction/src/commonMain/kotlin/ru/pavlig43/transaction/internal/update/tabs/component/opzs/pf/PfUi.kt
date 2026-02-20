package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class PfUi(
    val id: Int = 0,
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val movementId: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val count: Int = 0,
    val dateBorn: LocalDate = getCurrentLocalDate(),
) : ISingleLineTableUi

internal fun PfUi.toDto(): PfBD = PfBD(
    transactionId = transactionId,
    batchId = batchId,
    movementId = movementId,
    count = count,
    productId = productId,
    productName = productName,
    declarationId = declarationId,
    declarationName = declarationName,
    vendorName = vendorName,
    id = id
)

internal fun PfBD.toUi(): PfUi = PfUi(
    id = id,
    transactionId = transactionId,
    batchId = batchId,
    movementId = movementId,
    productId = productId,
    productName = productName,
    declarationId = declarationId,
    declarationName = declarationName,
    vendorName = vendorName,
    count = count,
)

