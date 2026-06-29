package ru.pavlig43.nocombro.mobile.experiments

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.add_circle
import ru.pavlig43.theme.delete

/**
 * Route mobile-фичи экспериментов.
 */
@Composable
fun ExperimentsRoute(
    component: ExperimentsMobileComponent,
    onOpenMenu: () -> Unit,
) {
    Children(
        stack = component.stack,
        modifier = Modifier.fillMaxSize(),
    ) { child ->
        when (val instance = child.instance) {
            is ExperimentsMobileChild.List -> ExperimentsListScreen(
                listComponent = instance.component,
                onSelectExperiment = component::selectExperiment,
                onOpenMenu = onOpenMenu,
            )

            is ExperimentsMobileChild.Details -> ExperimentDetailsScreen(
                detailsComponent = instance.component,
                onBack = component::closeExperimentDetails,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentsListScreen(
    listComponent: ExperimentsListComponent,
    onSelectExperiment: (Int) -> Unit,
    onOpenMenu: () -> Unit,
) {
    val experiments by listComponent.experiments.collectAsState()
    val showArchived by listComponent.showArchivedState.collectAsState()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text("Эксперименты") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Text("☰")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = listComponent::createExperiment,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.add_circle),
                    contentDescription = "Добавить",
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FilterChip(
                    selected = showArchived,
                    onClick = { listComponent.setArchivedMode(!showArchived) },
                    label = { Text("Архив") },
                )
            }
            items(experiments) { experiment ->
                ExperimentListItem(
                    experiment = experiment,
                    onClick = { onSelectExperiment(experiment.id) },
                    onDelete = { listComponent.deleteExperiment(experiment.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentDetailsScreen(
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
                                text = message,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            TextButton(onClick = detailsComponent::dismissMessage) {
                                Text("Закрыть")
                            }
                        }
                    }
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
                        isSelected = state.selectedEntry?.id == entry.id,
                        onClick = { detailsComponent.selectEntry(entry.id) },
                    )
                }
            }
            item {
                ExperimentEntryEditor(
                    selectedEntry = state.selectedEntry,
                    draft = state.entryDraft,
                    onDraftChange = detailsComponent::onEntryContentChange,
                )
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
                Text("Сегодня")
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
    isSelected: Boolean,
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
            if (isSelected) {
                Text(
                    text = "Выбрана",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ExperimentEntryEditor(
    selectedEntry: ExperimentEntryDetails?,
    draft: String,
    onDraftChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider()
        Text(
            text = selectedEntry?.let { "Запись: ${it.dateText}" } ?: "Выберите запись",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            value = draft,
            onValueChange = onDraftChange,
            enabled = selectedEntry != null,
            label = { Text("Текст записи") },
            minLines = 6,
        )
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

@Composable
private fun ReminderEditorDialog(
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

@Composable
private fun ExperimentListItem(
    experiment: MobileExperiment,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = experiment.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (experiment.ideaDescription.isNotBlank()) {
                    Text(
                        text = experiment.ideaDescription,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(Res.drawable.delete),
                    contentDescription = "Удалить",
                )
            }
        }
    }
}

