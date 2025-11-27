package ru.pavlig43.itemlist.api.component.refactoring

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.itemlist.internal.component.DeclarationListComponent
import ru.pavlig43.itemlist.internal.component.DocumentsListComponent
import ru.pavlig43.itemlist.internal.ui.DeclarationListScreen
import ru.pavlig43.itemlist.internal.ui.DocumentListScreen




@Composable
fun GeneralItemListScreen(
    component: ItemListFactoryComponent,
    modifier: Modifier = Modifier
){
    when(val listComponent = component.listComponent){
        is DeclarationListComponent -> DeclarationListScreen(listComponent)
        is DocumentsListComponent -> DocumentListScreen(listComponent)
    }

}


