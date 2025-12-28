package ru.pavlig43.immutable.internal.component.items.product

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object ProductSorter: SortMatcher<ProductTableUi, ProductField> {
    override fun sort(
        items: List<ProductTableUi>,
        sort: SortState<ProductField>?,
    ): List<ProductTableUi>{
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                ProductField.ID -> items.sortedBy { it.composeId }
                ProductField.NAME -> items.sortedBy { it.displayName.lowercase() }
                ProductField.TYPE -> items.sortedBy { it.type.enumValue.displayName.lowercase() }
                ProductField.CREATED_AT -> items.sortedBy { it.createdAt }
                ProductField.COMMENT -> items.sortedBy { it.comment.lowercase() }
                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}