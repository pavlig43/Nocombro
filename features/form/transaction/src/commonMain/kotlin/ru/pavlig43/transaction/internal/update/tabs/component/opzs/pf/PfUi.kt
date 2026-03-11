package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class PfUi(
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val movementId: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val count: DecimalData = DecimalData(0, DecimalFormat.Decimal3),
) : ISingleLineTableUi

internal fun PfUi.toDto(getDateBorn:()-> LocalDate): PfBD = PfBD(
    transactionId = transactionId,
    batchId = batchId,
    movementId = movementId,
    count = count.value,
    productId = productId,
    productName = productName,
    declarationId = declarationId,
    declarationName = declarationName,
    vendorName = vendorName,
    dateBorn = getDateBorn()
)

internal fun PfBD.toUi(transactionId: Int): PfUi = PfUi(
    transactionId = transactionId,
    batchId = batchId,
    movementId = movementId,
    productId = productId,
    productName = productName,
    declarationId = declarationId,
    declarationName = declarationName,
    vendorName = vendorName,
    count = DecimalData(count, DecimalFormat.Decimal3),
)

