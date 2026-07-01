package ru.pavlig43.nocombro.mobile.experiments.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentReminderEditorState

@Composable
internal fun ReminderEditorDialog(
    editor: ExperimentReminderEditorState,
    onTextChange: (String) -> Unit,
    onDateTimeChange: (LocalDateTime) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    var showDateTimePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editor.isEdit) "Напоминание" else "Новое напоминание") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = editor.text,
                    onValueChange = onTextChange,
                    label = { Text("Текст") },
                )
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDateTimePicker = true },
                ) {
                    Text(editor.reminderDateTime.format(dateTimeFormat))
                }
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        },
    )

    if (showDateTimePicker) {
        DateTimePickerDialog(
            dateTime = editor.reminderDateTime,
            onDismissRequest = { showDateTimePicker = false },
            onSelectDateTime = onDateTimeChange,
        )
    }
}
