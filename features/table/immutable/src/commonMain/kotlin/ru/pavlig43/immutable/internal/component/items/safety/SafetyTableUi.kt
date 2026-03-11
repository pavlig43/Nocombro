package ru.pavlig43.immutable.internal.component.items.safety

import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class SafetyTableUi(
    val productId: Int = 0,
    val productName: String = "",
    val vendorName: String = "",
    val count: DecimalData = DecimalData(0, DecimalFormat.Decimal3()),
    val reorderPoint: DecimalData = DecimalData(0, DecimalFormat.Decimal3()),
    val orderQuantity: DecimalData = DecimalData(0, DecimalFormat.Decimal3()),
    override val composeId: Int = 0,
) : IMultiLineTableUi
