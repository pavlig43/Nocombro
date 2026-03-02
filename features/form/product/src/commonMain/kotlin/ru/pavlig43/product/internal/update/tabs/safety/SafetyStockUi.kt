package ru.pavlig43.product.internal.update.tabs.safety

import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class SafetyStockUi(
    val reorderPoint: Int,
    val orderQuantity: Int
): ISingleLineTableUi
