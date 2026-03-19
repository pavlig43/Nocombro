package ru.pavlig43.profitability.internal.model

import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class ProfitabilityUi(
    val productId: Int,
    val productName: String,
    val quantity: DecimalData3,
    val revenue: DecimalData2,
    val expenses: DecimalData2,
    val expensesOnOneKg: DecimalData2,
    val profit: DecimalData2,
    val margin: Double,
    val profitability: Double,
    override val composeId: Int = productId
) : IMultiLineTableUi
