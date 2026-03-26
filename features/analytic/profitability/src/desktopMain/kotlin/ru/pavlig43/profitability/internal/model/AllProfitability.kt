package ru.pavlig43.profitability.internal.model

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.tablecore.model.IMultiLineTableUi

internal data class AllProfitability(
    val mainExpenses: DecimalData2,
    val products: List<ProfitabilityProduct>
)
internal data class ProfitabilityProduct(
    val productId: Int,
    val productName: String,
    val quantity: DecimalData3,
    val revenue: DecimalData2,
    val totalExpenses: DecimalData2,
    val expensesOnOneKg: DecimalData2,
    val profit: DecimalData2,
    val margin: Double,
    val profitability: Double,
    val details: List<ProfitabilityBatchDetails>,
    val expandedDetails: Boolean = false
): IMultiLineTableUi{
    override val composeId: Int
        get() = productId
}

internal data class ProfitabilityBatchDetails(
    val contrAgentId: Int,
    val contrAgentName: String,
    val date: LocalDate,
    val quantity: DecimalData3,
    val revenue: DecimalData2,
    val expenses: DecimalData2,
    val expensesOnOneKg: DecimalData2,
    val profit: DecimalData2,
    val margin: Double,
    val profitability: Double
)