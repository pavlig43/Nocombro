package ru.pavlig43.profitability.internal.component

import ru.pavlig43.profitability.internal.model.ProfitabilityBatchDetails
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object ProfitabilitySorter : SortMatcher<ProfitabilityProduct, ProfitabilityField> {
    override fun sort(
        items: List<ProfitabilityProduct>,
        sort: SortState<ProfitabilityField>?
    ): List<ProfitabilityProduct> {
        if (sort == null) return items

        val sorted = when (sort.column) {
            ProfitabilityField.PRODUCT_NAME -> items.sortedBy { it.productName }
            ProfitabilityField.QUANTITY -> items.sortedBy { it.quantity }
            ProfitabilityField.REVENUE -> items.sortedBy { it.revenue }
            ProfitabilityField.EXPENSES -> items.sortedBy { it.totalExpenses }
            ProfitabilityField.EXPENSES_ON_ONE_KG -> items.sortedBy { it.expensesOnOneKg }
            ProfitabilityField.PROFIT -> items.sortedBy { it.profit }
            ProfitabilityField.MARGIN -> items.sortedBy { it.margin }
            ProfitabilityField.PROFITABILITY -> items.sortedBy { it.profitability }
            else -> items
        }
        return if (sort.order == SortOrder.DESCENDING) sorted.asReversed() else sorted
    }
}
internal object ProfitabilityDetailSorter : SortMatcher<ProfitabilityBatchDetails, BatchDetailsField> {

    override fun sort(
        items: List<ProfitabilityBatchDetails>,
        sort: SortState<BatchDetailsField>?
    ): List<ProfitabilityBatchDetails> {
        if (sort == null) return items
        val sorted = when(sort.column){
            BatchDetailsField.CONTRAGENT_NAME -> items.sortedBy { it.contrAgentName }
            BatchDetailsField.DATE -> items.sortedBy { it.date }
            BatchDetailsField.QUANTITY -> items.sortedBy { it.quantity }
            BatchDetailsField.REVENUE -> items.sortedBy { it.revenue }
            BatchDetailsField.EXPENSES -> items.sortedBy { it.expenses }
            BatchDetailsField.EXPENSES_ON_ONE_KG -> items.sortedBy { it.expensesOnOneKg }
            BatchDetailsField.PROFIT -> items.sortedBy { it.profit }
            BatchDetailsField.MARGIN -> items.sortedBy { it.margin }
            BatchDetailsField.PROFITABILITY -> items.sortedBy { it.profitability }
        }
        return if (sort.order == SortOrder.DESCENDING) sorted.asReversed() else sorted
    }
}
