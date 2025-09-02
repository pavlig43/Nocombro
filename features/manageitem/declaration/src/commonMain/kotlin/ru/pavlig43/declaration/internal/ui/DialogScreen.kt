package ru.pavlig43.declaration.internal.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.declaration.api.component.MBSComponent
import ru.pavlig43.itemlist.api.ui.ItemListScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MBS(component: MBSComponent, modifier: Modifier = Modifier){
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = component::onDismissClicked,
        sheetState = sheetState,
        modifier = modifier.fillMaxSize()){
        ItemListScreen(component.documentList,Modifier.fillMaxSize())
    }
}
