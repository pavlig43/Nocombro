package ru.pavlig43.itemlist.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.itemlist.api.component.ItemListFactoryComponent
import ru.pavlig43.itemlist.internal.component.DeclarationListComponent
import ru.pavlig43.itemlist.internal.component.DocumentsListComponent
import ru.pavlig43.itemlist.internal.component.ProductListComponent
import ru.pavlig43.itemlist.internal.component.TransactionListComponent
import ru.pavlig43.itemlist.internal.component.VendorListComponent
import ru.pavlig43.itemlist.internal.ui.DeclarationListScreen
import ru.pavlig43.itemlist.internal.ui.DocumentListScreen
import ru.pavlig43.itemlist.internal.ui.ProductListScreen
import ru.pavlig43.itemlist.internal.ui.TransactionListScreen
import ru.pavlig43.itemlist.internal.ui.VendorListScreen


@Composable
fun GeneralItemListScreen(
    component: ItemListFactoryComponent,
){
    when(val listComponent = component.listComponent){
        is DeclarationListComponent -> DeclarationListScreen(listComponent)
        is DocumentsListComponent -> DocumentListScreen(listComponent)
        is ProductListComponent -> ProductListScreen(listComponent)
        is VendorListComponent -> VendorListScreen(listComponent)
        is TransactionListComponent -> TransactionListScreen(listComponent)
    }

}


