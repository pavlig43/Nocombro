package ru.pavlig43.immutable.internal.component.items.batch

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object BatchSorter :
    SortMatcher<BatchTableUi, BatchField> {
    override fun sort(
        items: List<BatchTableUi>,
        sort: SortState<BatchField>?,
    ): List<BatchTableUi> {
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                BatchField.ID -> items.sortedBy { it.composeId }
                BatchField.BATCH_ID -> items.sortedBy { it.batchId }
                BatchField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
                BatchField.COUNT -> items.sortedBy { it.balance }
                BatchField.DATE_BORN -> items.sortedBy { it.dateBorn }
                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
