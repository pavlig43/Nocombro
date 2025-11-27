package ru.pavlig43.itemlist.api.component.refactoring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.itemlist.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.api.ui.ItemListScreen
import ru.pavlig43.itemlist.internal.component.DeclarationListComponent
import ru.pavlig43.itemlist.internal.component.DocumentsListComponent
import ru.pavlig43.itemlist.internal.component.ProductListComponent
import ru.pavlig43.itemlist.internal.component.VendorListComponent
import ru.pavlig43.itemlist.internal.ui.DeclarationListScreen
import ru.pavlig43.itemlist.internal.ui.DocumentListScreen
import ru.pavlig43.itemlist.internal.ui.ProductListScreen
import ru.pavlig43.itemlist.internal.ui.VendorListScreen


@Composable
fun GeneralItemListScreen(
    component: ItemListFactoryComponent,
    modifier: Modifier = Modifier
){
    when(val listComponent = component.listComponent){
        is DeclarationListComponent -> DeclarationListScreen(listComponent)
        is DocumentsListComponent -> DocumentListScreen(listComponent)
        is ProductListComponent -> ProductListScreen(listComponent)
        is VendorListComponent -> VendorListScreen(listComponent)
    }

}


