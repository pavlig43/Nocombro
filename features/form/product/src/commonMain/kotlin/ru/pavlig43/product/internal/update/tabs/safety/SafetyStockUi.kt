package ru.pavlig43.product.internal.update.tabs.safety

import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class SafetyStockUi(
    val id: Int,
    val productId: Int,
    val reorderPoint: Int,
    val orderQuantity: Int
): ISingleLineTableUi

internal fun SafetyStockUi.toDto(): SafetyStock {
    return SafetyStock(
        productId = productId,
        reorderPoint = reorderPoint,
        orderQuantity = orderQuantity,
        id = id
    )
}
internal fun SafetyStock.toUi(): SafetyStockUi {
    return SafetyStockUi(
        productId = productId,
        reorderPoint = reorderPoint,
        orderQuantity = orderQuantity,
        id = id
    )
}
