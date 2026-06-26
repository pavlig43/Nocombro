package ru.pavlig43.nocombro.mobile.experiments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ExperimentsApp(component: ExperimentsMobileComponent) {
    val repository = component.repository
    val state by repository.state.collectAsState()

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            ExperimentsScreen(
                state = state,
                repository = repository,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentsScreen(
    state: ExperimentsMobileState,
    repository: ExperimentsRepository,
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Experiments") },
                actions = {
                    Button(onClick = { scope.launch { repository.sync() } }) {
                        Text("Sync")
                    }
                }
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SyncStatusLine(state.syncStatus)
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = repository::createExperiment) {
                        Text("Create")
                    }
                    FilterChip(
                        selected = state.showArchived,
                        onClick = repository::toggleArchivedVisibility,
                        label = { Text("Archive") },
                    )
                }
            }
            items(state.experiments.filter { it.isArchived == state.showArchived }) { experiment ->
                ExperimentListItem(
                    experiment = experiment,
                    selected = experiment.id == state.selectedExperiment?.id,
                    onClick = { repository.selectExperiment(experiment.id) },
                )
            }
            item {
                Divider()
            }
            item {
                ExperimentDetails(
                    state = state,
                    repository = repository,
                )
            }
        }
    }
}

@Composable
private fun SyncStatusLine(status: SyncStatus) {
    val text = when (status) {
        SyncStatus.Idle -> "Sync ready"
        SyncStatus.Running -> "Syncing..."
        is SyncStatus.Failed -> "Sync error: ${status.message}"
        is SyncStatus.Synced -> "Synced"
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun ExperimentListItem(
    experiment: MobileExperiment,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = experiment.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
            if (experiment.ideaDescription.isNotBlank()) {
                Text(
                    text = experiment.ideaDescription,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ExperimentDetails(
    state: ExperimentsMobileState,
    repository: ExperimentsRepository,
) {
    val experiment = state.selectedExperiment
    if (experiment == null) {
        Text("No selected experiment")
        return
    }

    var title by remember(experiment.id) { mutableStateOf(experiment.title) }
    var description by remember(experiment.id) { mutableStateOf(experiment.ideaDescription) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("Description") },
        )
        Row {
            Checkbox(
                checked = experiment.isArchived,
                onCheckedChange = { repository.setSelectedExperimentArchived(it) },
            )
            Text(
                text = "Archived",
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Button(
            onClick = {
                repository.updateSelectedExperiment(title, description)
            },
        ) {
            Text("Save")
        }
        Button(onClick = repository::createTodayEntry) {
            Text("Today entry")
        }
        EntryEditor(
            entry = state.selectedEntry,
            entries = state.entries.filter { it.experimentId == experiment.id },
            onSelectEntry = repository::selectEntry,
            onChange = repository::updateSelectedEntry,
        )
        ReminderEditor(
            reminders = state.reminders.filter { it.experimentId == experiment.id },
            onCreate = repository::createReminder,
            onDelete = repository::deleteReminder,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryEditor(
    entry: MobileExperimentEntry?,
    entries: List<MobileExperimentEntry>,
    onSelectEntry: (Int) -> Unit,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Journal", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach {
                FilterChip(
                    selected = it.id == entry?.id,
                    onClick = { onSelectEntry(it.id) },
                    label = { Text(it.entryDate.toString()) },
                )
            }
        }
        OutlinedTextField(
            value = entry?.content.orEmpty(),
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            label = { Text("Entry") },
        )
    }
}

@Composable
private fun ReminderEditor(
    reminders: List<MobileExperimentReminder>,
    onCreate: (String) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reminders", style = MaterialTheme.typography.titleMedium)
        reminders.forEach { reminder ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = reminder.text,
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = { onDelete(reminder.id) }) {
                    Text("Delete")
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                label = { Text("Text") },
            )
            Button(
                onClick = {
                    onCreate(text)
                    text = ""
                },
            ) {
                Text("Add")
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
