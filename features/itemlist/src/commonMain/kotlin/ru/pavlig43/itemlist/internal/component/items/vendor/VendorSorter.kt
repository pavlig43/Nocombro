package ru.pavlig43.itemlist.internal.component.items.vendor

import ru.pavlig43.itemlist.internal.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object VendorSorter: SortMatcher<VendorItemUi, VendorField> {
    override fun sort(
        items: List<VendorItemUi>,
        sort: SortState<VendorField>?,
    ): List<VendorItemUi>{
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                VendorField.ID -> items.sortedBy { it.id }
                VendorField.COMMENT -> items.sortedBy { it.comment.lowercase() }
                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}