package ru.pavlig43.itemlist.statik.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.itemlist.statik.api.component.StaticItemListFactoryComponent
import ru.pavlig43.itemlist.statik.internal.component.DeclarationStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.DocumentsStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.ProductStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.TransactionStaticListContainer
import ru.pavlig43.itemlist.statik.internal.component.VendorStaticListContainer
import ru.pavlig43.itemlist.statik.internal.ui.DeclarationListScreen
import ru.pavlig43.itemlist.statik.internal.ui.DocumentListScreen
import ru.pavlig43.itemlist.statik.internal.ui.ProductListScreen
import ru.pavlig43.itemlist.statik.internal.ui.TransactionListScreen
import ru.pavlig43.itemlist.statik.internal.ui.VendorListScreen


@Composable
fun GeneralItemListScreen(
    component: StaticItemListFactoryComponent,
){
    when(val listComponent = component.listComponent){
        is DeclarationStaticListContainer -> DeclarationListScreen(listComponent)
        is DocumentsStaticListContainer -> DocumentListScreen(listComponent)
        is ProductStaticListContainer -> ProductListScreen(listComponent)
        is VendorStaticListContainer -> VendorListScreen(listComponent)
        is TransactionStaticListContainer -> TransactionListScreen(listComponent)
    }

}


