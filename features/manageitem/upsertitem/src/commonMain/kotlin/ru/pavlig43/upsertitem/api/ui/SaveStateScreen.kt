package ru.pavlig43.upsertitem.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import ru.pavlig43.core.data.Item
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.upsertitem.api.component.ISaveItemComponent
import ru.pavlig43.upsertitem.api.component.SaveItemState
import ru.pavlig43.upsertitem.internal.RETRY
import ru.pavlig43.upsertitem.internal.SAVE
import ru.pavlig43.upsertitem.internal.ui.SaveDialog

@Composable
fun<I: Item> SaveScreenState(
    component: ISaveItemComponent<I>,
    modifier: Modifier = Modifier
){
    var saveDialogState by remember { mutableStateOf(false) }
    val saveState by component.saveState.collectAsState()
    val isValidValue by component.isValidValue.collectAsState()
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (saveState is SaveItemState.Error){
            Text(
                text = (saveState as SaveItemState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = {saveDialogState = true},
            enabled = isValidValue,
        ) {

            when(saveState){
                is SaveItemState.Error -> Text(RETRY)
                is SaveItemState.Init -> Text(SAVE)
                is SaveItemState.Loading -> ProgressIndicator(Modifier.size(24.dp))
                is SaveItemState.Success -> LaunchedEffect(Unit){component.onSuccessAction()}
            }
        }
    }

    if (saveDialogState){
        SaveDialog(
            onConfirmSave = {component.saveItem()},
            onDismissRequest = {saveDialogState = false}
        )
    }
}