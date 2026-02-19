package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

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
    return PfBD(
        transactionId = id,
        productId = productId,
        productName = productName,
        declarationId = declarationId,
        declarationName = declarationName,
        count = count,
        id = id
    )
}

internal fun PfBD.toUi(): PfUi {
    return PfUi(
        id = id,
        productId = productId,
        productName = productName,
        declarationId = declarationId,
        declarationName = declarationName,
        count = count
    )
}

