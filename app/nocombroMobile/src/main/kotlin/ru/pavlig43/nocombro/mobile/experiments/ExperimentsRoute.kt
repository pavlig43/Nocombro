package ru.pavlig43.nocombro.mobile.experiments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children

/**
 * Route mobile-фичи экспериментов.
 */
@Composable
fun ExperimentsRoute(
    component: ExperimentsMobileComponent,
    onOpenMenu: () -> Unit,
) {
    val state by component.state.collectAsState()

    Children(
        stack = component.stack,
        modifier = Modifier.fillMaxSize(),
    ) { child ->
        when (child.instance) {
            ExperimentsMobileChild.List -> ExperimentsListScreen(
                state = state,
                component = component,
                onOpenMenu = onOpenMenu,
            )

            is ExperimentsMobileChild.Details -> ExperimentDetailsScreen(
                state = state,
                component = component,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentsListScreen(
    state: ExperimentsMobileState,
    component: ExperimentsMobileComponent,
    onOpenMenu: () -> Unit,
) {
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
            ExtendedFloatingActionButton(
                onClick = component::createExperiment,
                text = { Text("Создать") },
                icon = { Text("+") },
            )
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
                SyncStatusLine(state.syncStatus)
            }
            item {
                FilterChip(
                    selected = state.showArchived,
                    onClick = component::toggleArchivedVisibility,
                    label = { Text("Архив") },
                )
            }
            items(state.experiments.filter { it.isArchived == state.showArchived }) { experiment ->
                ExperimentListItem(
                    experiment = experiment,
                    onClick = { component.selectExperiment(experiment.id) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExperimentDetailsScreen(
    state: ExperimentsMobileState,
    component: ExperimentsMobileComponent,
) {
    val title = state.selectedExperiment?.title?.ifBlank { "Эксперимент" } ?: "Эксперимент"

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = component::closeExperimentDetails) {
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
            item {
                ExperimentDetails(
                    state = state,
                    component = component,
                )
            }
        }
    }
}

@Composable
private fun SyncStatusLine(status: SyncStatus) {
    val text = when (status) {
        SyncStatus.Idle -> "Синхронизация не настроена"
        SyncStatus.Running -> "Идёт синхронизация"
        is SyncStatus.Failed -> "Ошибка синхронизации: ${status.message}"
        is SyncStatus.Synced -> "Синхронизировано"
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
    component: ExperimentsMobileComponent,
) {
    val experiment = state.selectedExperiment
    if (experiment == null) {
        Text("Эксперимент не выбран")
        return
    }

    var title by remember(experiment.id) { mutableStateOf(experiment.title) }
    var description by remember(experiment.id) { mutableStateOf(experiment.ideaDescription) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Название") },
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            label = { Text("Описание") },
        )
        Row {
            Checkbox(
                checked = experiment.isArchived,
                onCheckedChange = { component.setSelectedExperimentArchived(it) },
            )
            Text(
                text = "В архиве",
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Button(
            onClick = {
                component.updateSelectedExperiment(title, description)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Сохранить")
        }
        OutlinedButton(
            onClick = component::createTodayEntry,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Запись за сегодня")
        }
        EntryEditor(
            entry = state.selectedEntry,
            entries = state.entries.filter { it.experimentId == experiment.id },
            onSelectEntry = component::selectEntry,
            onChange = component::updateSelectedEntry,
        )
        ReminderEditor(
            reminders = state.reminders.filter { it.experimentId == experiment.id },
            onCreate = component::createReminder,
            onDelete = component::deleteReminder,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EntryEditor(
    entry: MobileExperimentEntry?,
    entries: List<MobileExperimentEntry>,
    onSelectEntry: (Int) -> Unit,
    onChange: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Журнал", style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
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
                label = { Text("Запись") },
            )
        }
    }
}

@Composable
private fun ReminderEditor(
    reminders: List<MobileExperimentReminder>,
    onCreate: (String) -> Unit,
    onDelete: (Int) -> Unit,
) {
    var text by remember { mutableStateOf("") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Напоминания", style = MaterialTheme.typography.titleMedium)
            reminders.forEach { reminder ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = reminder.text,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { onDelete(reminder.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Удалить")
                    }
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Текст") },
            )
            Button(
                onClick = {
                    onCreate(text)
                    text = ""
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Добавить")
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}
