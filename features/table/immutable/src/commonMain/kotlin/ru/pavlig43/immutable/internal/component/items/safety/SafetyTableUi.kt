package ru.pavlig43.immutable.internal.component.items.safety

import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class SafetyTableUi(
    val productId: Int = 0,
    val productName: String = "",
    val vendorName: String = "",
    val count: Int = 0,
    val reorderPoint: Int = 0,
    val orderQuantity: Int = 0,
    override val composeId: Int = 0,
) : IMultiLineTableUi
