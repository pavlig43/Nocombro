package ru.pavlig43.product.internal.update.tabs.composition

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object CompositionSorter : SortMatcher<CompositionUi, CompositionField> {
    override fun sort(
        items: List<CompositionUi>,
        sort: SortState<CompositionField>?
    ): List<CompositionUi> {
        if (sort == null) {
            return items
        }
        val sortedList =
            when (sort.column) {
                CompositionField.PRODUCT_NAME -> items.sortedBy { it.productName }
                CompositionField.COUNT -> items.sortedBy { it.count }
                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
