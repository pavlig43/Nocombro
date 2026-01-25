package ru.pavlig43.transaction.internal.component.tabs.component.buy

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object BuySorter: SortMatcher<BuyUi, BuyField> {

    override fun sort(
        items: List<BuyUi>,
        sort: SortState<BuyField>?
    ): List<BuyUi> {
        if (sort == null) {
            return items
        }
val sortedList = when(sort.column){
    BuyField.PRODUCT_NAME -> items.sortedBy { it.productName.lowercase() }
    BuyField.DECLARATION_NAME -> items.sortedBy { it.declarationName.lowercase() }
    BuyField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
    BuyField.DATE_BORN -> items.sortedBy { it.dateBorn }
    BuyField.PRICE -> items.sortedBy { it.price }
    BuyField.COMMENT -> items.sortedBy { it.comment.lowercase() }
    else -> items
}
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}