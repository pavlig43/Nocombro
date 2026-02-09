package ru.pavlig43.document.internal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.document.internal.component.CreateDocumentSingleLineComponent
import ru.pavlig43.mutable.api.component.singleLine.CreateState
import ru.pavlig43.mutable.api.ui.SingleLineScreen

/**
 * UI экран для создания документа через таблицу с одной строкой
 *
 * @param component Компонент создания документа
 */
@Composable
internal fun CreateDocumentSingleLineScreen(
    component: CreateDocumentSingleLineComponent
) {
    val dialog by component.dialog.subscribeAsState()
    val enabled by component.isValidFields.collectAsState()
    val createState by component.createState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        SingleLineScreen(component)

        CreateButton(
            onCreate = component::create,
            enabled = enabled,
            onSuccess = { component.onSuccessCreate(it) },
            createState = createState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    // Отображение диалога выбора даты
    dialog.child?.instance?.also { dateComponent ->
        ru.pavlig43.coreui.DatePickerDialog(dateComponent)
    }
}

@Composable
private fun CreateButton(
    onCreate: () -> Unit,
    enabled: Boolean,
    onSuccess: (Int) -> Unit,
    createState: CreateState,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Button(onClick = onCreate, enabled = enabled) {
            when (createState) {
                is CreateState.Error -> Box(Modifier.background(MaterialTheme.colorScheme.error)) {
                    Text("Повторить")
                }
                CreateState.Init -> Text("Создать")
                CreateState.Loading -> LoadingUi()
                is CreateState.Success -> {
                    // Автоматически переходим на обновление
                }
            }
        }
    }
}
