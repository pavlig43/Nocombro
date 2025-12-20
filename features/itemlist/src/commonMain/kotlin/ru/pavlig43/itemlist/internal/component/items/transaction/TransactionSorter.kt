package ru.pavlig43.itemlist.internal.component.items.transaction

import ru.pavlig43.itemlist.internal.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object TransactionSorter : SortMatcher<TransactionItemUi, TransactionField> {
    override fun sort(
        items: List<TransactionItemUi>,
        sort: SortState<TransactionField>?,
    ): List<TransactionItemUi> {
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                TransactionField.ID -> items.sortedBy { it.id }
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