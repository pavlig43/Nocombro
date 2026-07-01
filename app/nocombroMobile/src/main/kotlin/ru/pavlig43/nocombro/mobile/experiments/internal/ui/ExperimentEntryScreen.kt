package ru.pavlig43.nocombro.mobile.experiments.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentEntryComponent
import ru.pavlig43.nocombro.mobile.experiments.internal.component.MobileExperimentEntryFile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentEntryScreen(
    entryComponent: ExperimentEntryComponent,
    onBack: () -> Unit,
) {
    val state by entryComponent.uiState.collectAsState()
    val filePicker = rememberFilePickerLauncher { file ->
        file?.let(entryComponent::addFile)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(state.dateText.ifBlank { "Запись" }) },
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
                .padding(padding)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.errorMessage?.let { message ->
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
                            TextButton(onClick = entryComponent::dismissMessage) {
                                Text("Закрыть")
                            }
                        }
                    }
                }
            }
            item {
                ExperimentEntryFiles(
                    files = state.files,
                    isLoaded = state.isLoaded,
                    onAddClick = { filePicker.launch() },
                    onOpenClick = entryComponent::openFile,
                    onDeleteClick = entryComponent::deleteFile,
                )
            }
            item {
                ExperimentEntryEditor(
                    dateText = state.dateText,
                    content = state.content,
                    isLoaded = state.isLoaded,
                    onContentChange = entryComponent::onContentChange,
                )
            }
        }
    }
}

@Composable
private fun ExperimentEntryFiles(
    files: List<MobileExperimentEntryFile>,
    isLoaded: Boolean,
    onAddClick: () -> Unit,
    onOpenClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = "Файлы записи",
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(
                    onClick = onAddClick,
                    enabled = isLoaded,
                ) {
                    Text("Добавить")
                }
            }
            if (files.isEmpty()) {
                Text(
                    text = "Файлов нет",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                files.forEach { file ->
                    ExperimentEntryFileRow(
                        file = file,
                        onOpenClick = onOpenClick,
                        onDeleteClick = onDeleteClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExperimentEntryFileRow(
    file: MobileExperimentEntryFile,
    onOpenClick: (Int) -> Unit,
    onDeleteClick: (Int) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        HorizontalDivider()
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = "Локальный файл",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { onOpenClick(file.id) }) {
                    Text("Открыть")
                }
                TextButton(onClick = { onDeleteClick(file.id) }) {
                    Text("Удалить")
                }
            }
        }
    }
}

@Composable
private fun ExperimentEntryEditor(
    dateText: String,
    content: String,
    isLoaded: Boolean,
    onContentChange: (String) -> Unit,
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = if (isLoaded) "Текст записи" else "Загрузка записи",
                style = MaterialTheme.typography.titleMedium,
            )
            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                value = content,
                onValueChange = onContentChange,
                enabled = isLoaded,
                label = { Text(dateText.ifBlank { "Текст" }) },
                minLines = 7,
            )
        }
    }
}
