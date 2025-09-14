package ru.pavlig43.itemlist.api.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.itemlist.api.component.MBSItemListComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <I:Item,S:ItemType> MBS(component: MBSItemListComponent<I,S>, modifier: Modifier = Modifier){
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = component::onDismissClicked,
        sheetState = sheetState,
        modifier = modifier.fillMaxSize()){
        ItemListScreen(component.itemList, Modifier.fillMaxSize())
    }
}