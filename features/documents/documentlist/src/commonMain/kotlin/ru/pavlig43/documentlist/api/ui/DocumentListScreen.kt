package ru.pavlig43.documentlist.api.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.documentlist.api.component.IDocumentListComponent
import ru.pavlig43.itemlist.api.ui.ItemListScreen

@Composable
fun DocumentListScreen(
    component: IDocumentListComponent
){
    ItemListScreen(component.itemListComponent)

}