package ru.pavlig43.thermallabel.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.ProjectDialog
import ru.pavlig43.datetime.single.date.DatePickerDialog
import ru.pavlig43.datetime.single.date.DateRow
import ru.pavlig43.thermallabel.api.component.ThermalLabelDialogComponent
import ru.pavlig43.thermallabel.api.component.ThermalLabelInnerDialogChild
import ru.pavlig43.thermallabel.api.model.ThermalLabelSize

/**
 * Compose-диалог для настройки и запуска генерации термоэтикетки.
 *
 * Показывает размер, дату, массу, состояние загрузки prefill-данных
 * и возможные ошибки генерации.
 */
@Composable
fun ThermalLabelDialog(
    component: ThermalLabelDialogComponent,
) {
    val uiState by component.uiState.collectAsState()
    val message by component.message.collectAsState()
    val dialog by component.dialog.subscribeAsState()

    ProjectDialog(
        onDismissRequest = component.onDismissRequest,
        header = { Text("Этикетка") },
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LabelSizeDropdown(
                    selectedTitle = uiState.selectedSize.title,
                    onSelectSize = component::onSelectSize,
                    enabled = !uiState.isGenerating,
                )

                DateRow(
                    date = uiState.date,
                    isChangeDialogVisible = component::openDateDialog,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = uiState.massText,
                    onValueChange = component::onMassChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Масса, кг") },
                    enabled = !uiState.isGenerating,
                )

                if (uiState.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                message?.let { currentMessage ->
                    Text(
                        text = currentMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = component.onDismissRequest,
                        enabled = !uiState.isGenerating,
                    ) {
                        Text("Отмена")
                    }

                    Button(
                        onClick = component::generate,
                        enabled = !uiState.isLoading && !uiState.isGenerating,
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp),
                                strokeWidth = 2.dp,
                            )
                        }
                        Text("ОК")
                    }
                }
            }
        }
    )

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is ThermalLabelInnerDialogChild.Date -> {
                DatePickerDialog(dialogChild.component)
            }
        }
    }
}

@Composable
private fun LabelSizeDropdown(
    selectedTitle: String,
    onSelectSize: (ThermalLabelSize) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        OutlinedTextField(
            value = selectedTitle,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            enabled = enabled,
            label = { Text("Размер термочека") },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ThermalLabelSize.entries.forEach { size ->
                DropdownMenuItem(
                    text = { Text(size.title) },
                    onClick = {
                        onSelectSize(size)
                        expanded = false
                    },
                )
            }
        }
        TextButton(
            onClick = { expanded = true },
            enabled = enabled,
        ) {
            Text("Выбрать размер")
        }
    }
}
