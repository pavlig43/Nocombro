package ru.pavlig43.documentlist.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.documentlist.api.component.IDocumentListComponent
import ru.pavlig43.itemlist.api.ui.ItemListScreen

@Composable
fun DocumentScreen(
    component: IDocumentListComponent
){
    ItemListScreen(component.itemListComponent)

}