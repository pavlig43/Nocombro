package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class PfUi(
    val id: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val declarationId: Int = 0,
    val declarationName: String = "",
    val count: Int = 0,
) : ISingleLineTableUi

internal fun PfUi.toDto(): PfBD {
    val now = getCurrentLocalDate()
    return PfBD(
        transactionId = id,
        batchId = 0,
        movementId = 0,
        count = count,
        productId = productId,
        productName = productName,
        declarationId = declarationId,
        declarationName = declarationName,
        dateBorn = now,
        id = id
    )
}

internal fun PfBD.toUi(): PfUi = PfUi(
    id = id,
    productId = productId,
    productName = productName,
    declarationId = declarationId,
    declarationName = declarationName,
    count = count
)

