package ru.pavlig43.profitability.internal.component

import ru.pavlig43.profitability.internal.model.ProfitabilityUi
import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object ProfitabilitySorter : SortMatcher<ProfitabilityUi, ProfitabilityField> {
    override fun sort(
        items: List<ProfitabilityUi>,
        sort: SortState<ProfitabilityField>?
    ): List<ProfitabilityUi> {
        if (sort == null) return items

        val sorted = when (sort.column) {
            ProfitabilityField.NAME -> items.sortedBy { it.productName }
            ProfitabilityField.QUANTITY -> items.sortedBy { it.quantity }
            ProfitabilityField.REVENUE -> items.sortedBy { it.revenue }
            ProfitabilityField.EXPENSES -> items.sortedBy { it.expenses }
            ProfitabilityField.EXPENSES_ON_ONE_KG -> items.sortedBy { it.expensesOnOneKg }
            ProfitabilityField.PROFIT -> items.sortedBy { it.profit }
            ProfitabilityField.MARGIN -> items.sortedBy { it.margin }
            ProfitabilityField.PROFITABILITY -> items.sortedBy { it.profitability }
            else -> items
        }
        return if (sort.order == SortOrder.DESCENDING) sorted.asReversed() else sorted
    }
}
