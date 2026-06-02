package ru.pavlig43.experiments.api.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.pavlig43.experiments.api.component.ExperimentEntryListItem
import ru.pavlig43.experiments.api.component.ExperimentListItem
import ru.pavlig43.experiments.api.component.ExperimentsComponent
import ru.pavlig43.files.api.component.FilesComponent
import ru.pavlig43.files.api.ui.FilesScreen

@Composable
fun ExperimentsScreen(
    component: ExperimentsComponent,
    modifier: Modifier = Modifier,
) {
    val uiState by component.uiState.collectAsState()
    val draft by component.experimentDraft.collectAsState()
    val entryDraft by component.entryDraft.collectAsState()
    val message by component.message.collectAsState()
    val filesComponent by component.filesComponent.collectAsState()

    message?.let { text ->
        AlertDialog(
            onDismissRequest = component::dismissMessage,
            confirmButton = {
                Button(onClick = component::dismissMessage) {
                    Text("Закрыть")
                }
            },
            title = { Text("Ошибка") },
            text = { Text(text) },
        )
    }

    Row(
        modifier = modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExperimentListPane(
            experiments = uiState.experiments,
            selectedExperimentId = uiState.selectedExperiment?.id,
            showArchived = uiState.showArchived,
            onToggleArchived = component::toggleArchived,
            onCreateExperiment = component::createExperiment,
            onSelectExperiment = component::selectExperiment,
            modifier = Modifier.width(280.dp).fillMaxHeight(),
        )
        ExperimentDetailsPane(
            title = draft.title,
            ideaDescription = draft.ideaDescription,
            updatedAtText = uiState.selectedExperiment?.updatedAtText,
            hasSelection = uiState.selectedExperiment != null,
            isArchived = uiState.selectedExperiment?.isArchived == true,
            entries = uiState.entries,
            selectedEntryId = uiState.selectedEntry?.id,
            onTitleChange = component::onTitleChange,
            onIdeaDescriptionChange = component::onIdeaDescriptionChange,
            onOpenToday = component::openTodayEntry,
            onCreateTodayEntry = component::createTodayEntry,
            onToggleArchive = component::toggleArchiveSelected,
            onSelectEntry = component::selectEntry,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        EntryEditorPane(
            title = uiState.selectedEntry?.dateText,
            content = entryDraft,
            showEmptyState = uiState.selectedExperiment != null && uiState.selectedEntry == null,
            onContentChange = component::onEntryContentChange,
            files = filesComponent,
            modifier = Modifier.weight(1.1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun ExperimentListPane(
    experiments: List<ExperimentListItem>,
    selectedExperimentId: Int?,
    showArchived: Boolean,
    onToggleArchived: (Boolean) -> Unit,
    onCreateExperiment: () -> Unit,
    onSelectExperiment: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Эксперименты", style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onToggleArchived(false) },
                    enabled = showArchived,
                ) {
                    Text("Активные")
                }
                OutlinedButton(
                    onClick = { onToggleArchived(true) },
                    enabled = !showArchived,
                ) {
                    Text("Архив")
                }
            }
            Button(
                onClick = onCreateExperiment,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Новый эксперимент")
            }
            if (experiments.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Пока пусто.\nСоздай первый эксперимент.")
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    experiments.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectExperiment(item.id) }
                                .background(
                                    if (item.id == selectedExperimentId) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(item.title, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Обновлен: ${item.updatedAtText}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperimentDetailsPane(
    title: String,
    ideaDescription: String,
    updatedAtText: String?,
    hasSelection: Boolean,
    isArchived: Boolean,
    entries: List<ExperimentEntryListItem>,
    selectedEntryId: Int?,
    onTitleChange: (String) -> Unit,
    onIdeaDescriptionChange: (String) -> Unit,
    onOpenToday: () -> Unit,
    onCreateTodayEntry: () -> Unit,
    onToggleArchive: () -> Unit,
    onSelectEntry: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        if (!hasSelection) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("Выбери эксперимент слева")
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Название") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = ideaDescription,
                    onValueChange = onIdeaDescriptionChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Идея") },
                    minLines = 6,
                )
                updatedAtText?.let {
                    Text(
                        "Последнее обновление: $it",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onOpenToday) {
                        Text("Сегодня")
                    }
                    OutlinedButton(onClick = onToggleArchive) {
                        Text(if (isArchived) "Вернуть в активные" else "В архив")
                    }
                }
                HorizontalDivider()
                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Записей еще нет")
                            Button(onClick = onCreateTodayEntry) {
                                Text("Создать запись за сегодня")
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Дни", style = MaterialTheme.typography.titleMedium)
                        entries.forEach { item ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectEntry(item.id) }
                                    .background(
                                        if (item.id == selectedEntryId) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        }
                                    )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(item.dateText, fontWeight = FontWeight.SemiBold)
                                    if (item.preview.isNotBlank()) {
                                        Text(
                                            item.preview,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryEditorPane(
    title: String?,
    content: String,
    showEmptyState: Boolean,
    onContentChange: (String) -> Unit,
    files: FilesComponent?,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        when {
            title == null && showEmptyState -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Создай или выбери запись дня")
                }
            }

            title == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Выбери эксперимент и день")
                }
            }

            else -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = onContentChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Запись дня") },
                        minLines = 12,
                    )
                    Text("Файлы этого дня", style = MaterialTheme.typography.titleMedium)
                    files?.let {
                        FilesScreen(
                            component = it,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } ?: Text("Файлы станут доступны после выбора записи")
                }
            }
        }
    }
}
