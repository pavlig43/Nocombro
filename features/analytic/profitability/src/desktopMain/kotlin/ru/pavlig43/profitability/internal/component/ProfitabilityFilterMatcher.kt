package ru.pavlig43.profitability.internal.component

import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object ProfitabilityFilterMatcher : FilterMatcher<ProfitabilityProduct, ProfitabilityField>() {
    override fun matchesRules(
        item: ProfitabilityProduct,
        column: ProfitabilityField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            ProfitabilityField.PRODUCT_NAME -> matchesTextField(item.productName, stateAny)
            ProfitabilityField.QUANTITY -> matchesDecimalField(item.quantity,stateAny)
            ProfitabilityField.REVENUE -> matchesDecimalField(item.revenue,stateAny)
            ProfitabilityField.EXPENSES -> matchesDecimalField(item.totalExpenses,stateAny)
            ProfitabilityField.EXPENSES_ON_ONE_KG -> matchesDecimalField(item.expensesOnOneKg,stateAny)
            ProfitabilityField.PROFIT -> matchesDecimalField(item.profit,stateAny)
            ProfitabilityField.MARGIN -> matchesDoubleField(item.margin,stateAny)
            ProfitabilityField.PROFITABILITY ->  matchesDoubleField(item.profitability,stateAny)
            else -> true
        }
    }
}
