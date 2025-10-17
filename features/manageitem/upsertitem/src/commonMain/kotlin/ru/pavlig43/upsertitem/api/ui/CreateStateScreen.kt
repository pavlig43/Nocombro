package ru.pavlig43.upsertitem.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.coreui.ProgressIndicator
import ru.pavlig43.upsertitem.api.component.CreateState
import ru.pavlig43.upsertitem.api.component.ICreateComponent
import ru.pavlig43.upsertitem.internal.RETRY

@Composable
fun<I: Any> CreateStateScreen(
    component: ICreateComponent<I>,
    modifier: Modifier = Modifier,
){
    val saveState by component.saveState.collectAsState()
    val isValidValue by component.isValidValue.collectAsState()
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (saveState is CreateState.Error){
            Text(
                text = (saveState as CreateState.Error).message,
                color = MaterialTheme.colorScheme.error
            )
        }
        Button(
            onClick = component::createItem,
            enabled = isValidValue,
        ) {

            when(saveState){
                is CreateState.Error -> Text(RETRY)
                is CreateState.Init -> Text("Создать")
                is CreateState.Loading -> ProgressIndicator(Modifier.size(24.dp))
                is CreateState.Success -> {}
            }
        }
    }

}