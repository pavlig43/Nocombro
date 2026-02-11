package ru.pavlig43.update.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.ProjectDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SaveDialog(
    onConfirmSave: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ProjectDialog(
        onDismissRequest = onDismissRequest,
        header = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Сохранить и закрыть эту вкладку?",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }

        }
    ){
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                onConfirmSave()
                onDismissRequest()
            },
                modifier = Modifier.weight(1f)
            ) {
                Text("OK")
            }
            Button(onDismissRequest,Modifier.weight(1f)) {
                Text("Нет")
            }
        }
    }


}