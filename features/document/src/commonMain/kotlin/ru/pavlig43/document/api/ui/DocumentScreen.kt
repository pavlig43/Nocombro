package ru.pavlig43.document.api.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.document.api.component.IDocumentComponent
import ru.pavlig43.itemlist.api.ui.ItemListScreen

@Composable
fun DocumentScreen(
    component: IDocumentComponent
){
    val stack by component.stack.subscribeAsState()
    Children(
        stack = stack
    ){ child, ->
        when(val instance = child.instance){
            is IDocumentComponent.Child.ItemList -> ItemListScreen(instance.component)
        }
    }
}