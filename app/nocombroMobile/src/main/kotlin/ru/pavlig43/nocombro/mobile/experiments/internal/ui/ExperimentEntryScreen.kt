package ru.pavlig43.nocombro.mobile.experiments.internal.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import ru.pavlig43.nocombro.mobile.experiments.internal.component.ExperimentEntryComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentEntryScreen(
    entryComponent: ExperimentEntryComponent,
    onBack: () -> Unit,
) {
    val state by entryComponent.uiState.collectAsState()

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.errorMessage?.let { message ->
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
            ExperimentEntryEditor(
                dateText = state.dateText,
                content = state.content,
                isLoaded = state.isLoaded,
                onContentChange = entryComponent::onContentChange,
            )
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
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HorizontalDivider()
        Text(
            text = if (isLoaded) "Запись: $dateText" else "Загрузка записи",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp),
            value = content,
            onValueChange = onContentChange,
            enabled = isLoaded,
            label = { Text("Текст записи") },
            minLines = 6,
        )
    }
}
