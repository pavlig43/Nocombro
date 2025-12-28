//package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.component
//
//import ru.pavlig43.tablecore.utils.SortMatcher
//import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.BuyBaseUi
//import ua.wwind.table.data.SortOrder
//import ua.wwind.table.state.SortState
//
//internal object BuyBaseSorter: SortMatcher<BuyBaseUi, BuyBaseField> {
//
//    override fun sort(
//        items: List<BuyBaseUi>,
//        sort: SortState<BuyBaseField>?
//    ): List<BuyBaseUi> {
//        if (sort == null) {
//            return items
//        }
//val sortedList = when(sort.column){
//    BuyBaseField.PRODUCT_NAME -> items.sortedBy { it.productName.lowercase() }
//    BuyBaseField.DECLARATION_NAME -> items.sortedBy { it.declarationName.lowercase() }
//    BuyBaseField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
//    BuyBaseField.DATE_BORN -> items.sortedBy { it.dateBorn }
//    BuyBaseField.PRICE -> items.sortedBy { it.price }
//    BuyBaseField.COMMENT -> items.sortedBy { it.comment.lowercase() }
//    else -> items
//}
//        return if (sort.order == SortOrder.DESCENDING) {
//            sortedList.asReversed()
//        } else {
//            sortedList
//        }
//    }
//}