package ru.pavlig43.transaction.internal.update.tabs.component.sale

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object SaleSorter : SortMatcher<SaleUi, SaleField> {

    override fun sort(
        items: List<SaleUi>,
        sort: SortState<SaleField>?
    ): List<SaleUi> {
        if (sort == null) {
            return items
        }
        val sortedList = when (sort.column) {
            SaleField.PRODUCT_NAME -> items.sortedBy { it.productName.lowercase() }
            SaleField.CLIENT_NAME -> items.sortedBy { it.clientName.lowercase() }
            SaleField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
            SaleField.DATE_BORN -> items.sortedBy { it.dateBorn }
            SaleField.PRICE -> items.sortedBy { it.price }
            SaleField.COMMENT -> items.sortedBy { it.comment.lowercase() }
            else -> items
        }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
