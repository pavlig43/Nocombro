package ru.pavlig43.profitability.internal.component

import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.profitability.internal.model.ProfitabilityUi
import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object ProfitabilityFilterMatcher : FilterMatcher<ProfitabilityUi, ProfitabilityField>() {
    override fun matchesRules(
        item: ProfitabilityUi,
        column: ProfitabilityField,
        state: TableFilterState<*>
    ): Boolean {
        return when (column) {
            ProfitabilityField.SELECTION -> true
            ProfitabilityField.NAME -> matchesTextField(item.productName, state)
            else -> matchesDecimalField(
                when (column) {
                    ProfitabilityField.QUANTITY -> DecimalData3(item.quantity)
                    ProfitabilityField.REVENUE -> DecimalData2(item.revenue)
                    ProfitabilityField.EXPENSES -> DecimalData2(item.expenses)
                    ProfitabilityField.EXPENSES_ON_ONE_KG -> DecimalData2(item.expensesOnOneKg)
                    ProfitabilityField.PROFIT -> DecimalData2(item.profit)
                    ProfitabilityField.MARGIN -> DecimalData2((item.margin * 100).toInt())
                    ProfitabilityField.PROFITABILITY -> DecimalData2((item.profitability * 100).toInt())
                    else -> return true
                },
                state
            )
        }
    }
}
