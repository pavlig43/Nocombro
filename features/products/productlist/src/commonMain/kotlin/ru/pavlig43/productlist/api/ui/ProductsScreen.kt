package ru.pavlig43.productlist.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.productlist.api.component.IProductListComponent
import ru.pavlig43.itemlist.api.ui.ItemListScreen

@Composable
fun ProductsScreen(
    component: IProductListComponent
){
    ItemListScreen(component.itemListComponent)

}