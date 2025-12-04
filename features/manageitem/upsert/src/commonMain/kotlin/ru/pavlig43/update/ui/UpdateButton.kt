package ru.pavlig43.update.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.update.component.UpdateComponent
import ru.pavlig43.update.component.UpdateState

@Composable
internal fun UpdateButton(
    component: UpdateComponent,
    modifier: Modifier = Modifier,
) {
    var saveDialogState by remember { mutableStateOf(false) }
    val updateState by component.updateState.collectAsState()
    val isValidValue by component.isValidValue.collectAsState()
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (updateState is UpdateState.Error) {
            TextField(
                value = (updateState as UpdateState.Error).message,
                onValueChange = {  },
            )
            Text(
                text = (updateState as UpdateState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
        if (saveDialogState) {
            SaveDialog(
                onConfirmSave = component::onUpdate,
                onDismissRequest = { saveDialogState = false },
            )
        }
        Button(
            onClick = { saveDialogState = true },
            enabled = isValidValue,
        ) {

            when (updateState) {
                is UpdateState.Error -> Text("Повторить")
                is UpdateState.Init -> Text("Обновить")
                is UpdateState.Loading -> ProgressIndicator(Modifier.size(24.dp))
                is UpdateState.Success -> {
                    LaunchedEffect(Unit) { component.closeFormScreen() }
                }
            }
        }
    }

}