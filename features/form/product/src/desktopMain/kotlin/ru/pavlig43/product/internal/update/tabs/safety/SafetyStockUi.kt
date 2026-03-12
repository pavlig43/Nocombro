package ru.pavlig43.product.internal.update.tabs.safety

import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class SafetyStockUi(
    val id: Int,
    val productId: Int,
    val reorderPoint: DecimalData,
    val orderQuantity: DecimalData
): ISingleLineTableUi

internal fun SafetyStockUi.toDto(): SafetyStock {
    return SafetyStock(
        productId = productId,
        reorderPoint = reorderPoint.value,
        orderQuantity = orderQuantity.value,
        id = id
    )
}
internal fun SafetyStock.toUi(): SafetyStockUi {
    return SafetyStockUi(
        productId = productId,
        reorderPoint = DecimalData(reorderPoint, DecimalFormat.Decimal3),
        orderQuantity = DecimalData(orderQuantity, DecimalFormat.Decimal3),
        id = id
    )
}
