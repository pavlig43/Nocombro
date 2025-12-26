package ru.pavlig43.itemlist.internal.component.items.transaction

import ru.pavlig43.itemlist.internal.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object TransactionSorter : SortMatcher<TransactionTableUi, TransactionField> {
    override fun sort(
        items: List<TransactionTableUi>,
        sort: SortState<TransactionField>?,
    ): List<TransactionTableUi> {
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                TransactionField.ID -> items.sortedBy { it.composeId }
                TransactionField.COMMENT -> items.sortedBy { it.comment.lowercase() }
                TransactionField.CREATED_AT -> items.sortedBy { it.createdAt }
                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}