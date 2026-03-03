package ru.pavlig43.immutable.internal.component.items.safety

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object SafetySorter : SortMatcher<SafetyTableUi, SafetyField> {
    override fun sort(
        items: List<SafetyTableUi>,
        sort: SortState<SafetyField>?,
    ): List<SafetyTableUi> {
        if (sort == null) return items

        val sortedList = when (sort.column) {
            SafetyField.ID -> items.sortedBy { it.composeId }
            SafetyField.PRODUCT_NAME -> items.sortedBy { it.productName.lowercase() }
            SafetyField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
            SafetyField.COUNT -> items.sortedBy { it.count }
            SafetyField.REORDER_POINT -> items.sortedBy { it.reorderPoint }
            SafetyField.ORDER_QUANTITY -> items.sortedBy { it.orderQuantity }
        }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
