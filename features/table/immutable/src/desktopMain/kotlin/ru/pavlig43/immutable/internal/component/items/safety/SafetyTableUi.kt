package ru.pavlig43.immutable.internal.component.items.safety

import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class SafetyTableUi(
    val productId: Int = 0,
    val productName: String = "",
    val vendorName: String = "",
    val count: DecimalData3 = DecimalData3(0),
    val reorderPoint: DecimalData3 = DecimalData3(0),
    val orderQuantity: DecimalData3 = DecimalData3(0),
    override val composeId: Int = 0,
) : IMultiLineTableUi
