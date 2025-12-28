//package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy
//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.collectAsState
//import androidx.compose.runtime.getValue
//import ru.pavlig43.itemlist.internal.ui.MyTable
//
//@Composable
//fun BuyBaseTable(
//    component: BuyBaseProductFormSlot
//) {
//    val items by component.itemFields.collectAsState()
//    val tableData by component.tableData.collectAsState()
//    MyTable(
//        columns = component.columns,
//        items = items,
//        onFiltersChanged = component::updateFilters,
//        onEvent = {  },
//        onSortChanged = component::updateSort,
//        onRowClick = {},
//        tableData = tableData
//    )
//}