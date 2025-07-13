package ru.pavlig43.documentform.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.addfile.api.ui.AddFileScreen
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.coreui.tooltip.ProjectToolTip
import ru.pavlig43.createitem.api.ui.CreateBaseRowsOfItem
import ru.pavlig43.documentform.api.component.IDocumentFormComponent
import ru.pavlig43.documentform.api.component.SaveDocumentState
import ru.pavlig43.documentform.internal.ui.RETRY
import ru.pavlig43.documentform.internal.ui.SAVE_DOCUMENT
import ru.pavlig43.documentform.internal.ui.SaveDialog

@Composable
fun DocumentFormScreen(
    component: IDocumentFormComponent,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val isValidAllValue by component.isValidAllValue.collectAsState()
    val saveState by component.saveDocumentState.collectAsState()
    var saveDialogState by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
    ) {

        CreateBaseRowsOfItem(component = component.createBaseRowsOfComponent)
        AddFileScreen(component = component.addFileComponent)


        Button(
            onClick = {saveDialogState = true},
            enabled = isValidAllValue
        ) {
            when(val state = saveState){
                is SaveDocumentState.Error -> Column(){
                    Text(RETRY)
                    Text(state.message)
                }
                is SaveDocumentState.Init -> Text(SAVE_DOCUMENT)
                is SaveDocumentState.Loading -> ProgressIndicator(Modifier.size(24.dp))
                is SaveDocumentState.Success -> LaunchedEffect(Unit){component.closeScreen()}
            }
        }
        if (saveDialogState){
            SaveDialog(
                onConfirmSave = component::saveDocument,
                onDismissRequest = {saveDialogState = false}
            )
        }

    }


}