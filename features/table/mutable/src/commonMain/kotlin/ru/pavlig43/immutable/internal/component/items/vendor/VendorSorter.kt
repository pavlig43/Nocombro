package ru.pavlig43.immutable.internal.component.items.vendor

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object VendorSorter: SortMatcher<VendorTableUi, VendorField> {
    override fun sort(
        items: List<VendorTableUi>,
        sort: SortState<VendorField>?,
    ): List<VendorTableUi>{
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                VendorField.ID -> items.sortedBy { it.composeId }
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