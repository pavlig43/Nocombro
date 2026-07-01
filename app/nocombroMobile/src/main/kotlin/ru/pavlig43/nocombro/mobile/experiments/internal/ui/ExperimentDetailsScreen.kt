package ru.pavlig43.nocombro.mobile.experiments.internal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentDetailsComponent
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentDetailsUiState
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentEntryListItem
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentReminderListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentDetailsScreen(
    detailsComponent: ExperimentDetailsComponent,
    onBack: () -> Unit,
) {
    val state by detailsComponent.uiState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Эксперимент") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("‹")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { message ->
                item {
                    MessageCard(
                        text = message,
                        onDismiss = detailsComponent::dismissMessage,
                    )
                }
            }
            item {
                ExperimentFieldsSection(
                    state = state,
                    onTitleChange = detailsComponent::onTitleChange,
                    onIdeaChange = detailsComponent::onIdeaChange,
                    onOpenToday = detailsComponent::openTodayEntry,
                    onToggleArchive = detailsComponent::toggleArchive,
                )
            }
            item {
                Text(
                    text = "Записи",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (state.entries.isEmpty()) {
                item {
                    Text(
                        text = "Записей пока нет",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(state.entries) { entry ->
                    ExperimentEntryItem(
                        entry = entry,
                        onClick = { detailsComponent.openEntryScreen(entry.id) },
                    )
                }
            }
            item {
                ReminderSectionHeader(
                    onCreateReminder = detailsComponent::openCreateReminder,
                )
            }
            if (state.reminders.isEmpty()) {
                item {
                    Text(
                        text = "Напоминаний пока нет",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                items(state.reminders) { reminder ->
                    ExperimentReminderItem(
                        reminder = reminder,
                        onEdit = { detailsComponent.openEditReminder(reminder.id) },
                        onDelete = { detailsComponent.deleteReminder(reminder.id) },
                    )
                }
            }
        }
    }

    state.reminderEditor?.let { editor ->
        ReminderEditorDialog(
            editor = editor,
            onTextChange = detailsComponent::onReminderTextChange,
            onDateTimeChange = detailsComponent::onReminderDateTimeChange,
            onDismiss = detailsComponent::dismissReminderEditor,
            onSave = detailsComponent::saveReminder,
        )
    }
}

@Composable
private fun MessageCard(
    text: String,
    onDismiss: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = text,
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
private fun ExperimentFieldsSection(
    state: ExperimentDetailsUiState,
    onTitleChange: (String) -> Unit,
    onIdeaChange: (String) -> Unit,
    onOpenToday: () -> Unit,
    onToggleArchive: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            value = state.experimentDraft.title,
            onValueChange = onTitleChange,
            label = { Text("Название") },
            singleLine = true,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp),
            value = state.experimentDraft.ideaDescription,
            onValueChange = onIdeaChange,
            label = { Text("Идея") },
            minLines = 3,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = onOpenToday,
            ) {
                Text("Новая запись")
            }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = onToggleArchive,
            ) {
                Text(if (state.experiment?.isArchived == true) "Вернуть" else "Архив")
            }
        }
        state.experiment?.let { experiment ->
            Text(
                text = "Обновлено: ${experiment.updatedAtText}",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun ExperimentEntryItem(
    entry: ExperimentEntryListItem,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = entry.dateText,
                style = MaterialTheme.typography.titleSmall,
            )
            if (entry.preview.isNotBlank()) {
                Text(
                    text = entry.preview,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ReminderSectionHeader(
    onCreateReminder: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = "Напоминания",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(onClick = onCreateReminder) {
            Text("Добавить")
        }
    }
}

@Composable
private fun ExperimentReminderItem(
    reminder: ExperimentReminderListItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = reminder.text,
                style = MaterialTheme.typography.titleSmall,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = reminder.reminderDateTimeText,
                    style = MaterialTheme.typography.bodyMedium,
                )
                reminder.status?.let { status ->
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(onClick = onEdit) {
                    Text("Изменить")
                }
                OutlinedButton(onClick = onDelete) {
                    Text("Удалить")
                }
            }
        }
    }
}
