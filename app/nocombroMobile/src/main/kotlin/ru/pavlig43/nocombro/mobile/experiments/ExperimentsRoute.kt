package ru.pavlig43.nocombro.mobile.experiments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.theme.Res
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
            ExtendedFloatingActionButton(
                onClick = listComponent::createExperiment,
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
    onBack: () -> Unit,
) {
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
            item {
                Text("Детали эксперимента добавим отдельным локальным компонентом")
            }
        }
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

