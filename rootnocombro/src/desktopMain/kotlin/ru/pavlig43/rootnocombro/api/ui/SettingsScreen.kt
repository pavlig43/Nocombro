package ru.pavlig43.rootnocombro.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.coreui.ProjectDialog
import ru.pavlig43.coreui.ValidationErrorsCard
import ru.pavlig43.rootnocombro.api.component.LocalOrphanFile
import ru.pavlig43.rootnocombro.api.component.SettingsComponent
import ru.pavlig43.rootnocombro.api.component.SettingsOrphanFilesLoadState
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.settings

@Composable
fun SettingsScreen(
    component: SettingsComponent,
) {
    val isOpened by component.isSettingsOpened.collectAsState()
    if (!isOpened) {
        return
    }

    val darkMode by component.darkMode.collectAsState()
    val orphanFilesState by component.orphanFilesState.collectAsState()
    val orphanFilesActionError by component.orphanFilesActionError.collectAsState()

    ProjectDialog(
        onDismissRequest = component::closeSettings,
        header = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painter = painterResource(Res.drawable.settings),
                    contentDescription = null,
                )
                Text("Настройки")
            }
        },
        content = {
            Column(
                modifier = Modifier
                    .width(920.dp)
                    .heightIn(max = 720.dp)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SettingsThemeCard(
                    darkMode = darkMode,
                    onToggleDarkMode = component::toggleDarkMode,
                )
                SettingsOrphanFilesCard(
                    state = orphanFilesState,
                    actionError = orphanFilesActionError,
                    onDismissActionError = component::dismissOrphanFilesActionError,
                    onRefresh = component::refreshOrphanFiles,
                    onOpen = component::openOrphanFile,
                    onDelete = component::deleteOrphanFile,
                    onDeleteAll = component::deleteAllOrphanFiles,
                )
            }
        }
    )
}

@Composable
private fun SettingsThemeCard(
    darkMode: Boolean,
    onToggleDarkMode: () -> Unit,
) {
    SettingsSectionCard(
        title = "Тема",
        subtitle = "Быстрое переключение оформления приложения.",
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (darkMode) "Тёмная тема" else "Светлая тема",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = "Настройка применяется сразу ко всему приложению.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = darkMode,
                onCheckedChange = { onToggleDarkMode() },
            )
        }
    }
}

@Composable
private fun SettingsOrphanFilesCard(
    state: SettingsOrphanFilesLoadState,
    actionError: String?,
    onDismissActionError: () -> Unit,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit,
) {
    SettingsSectionCard(
        title = when (state) {
            is SettingsOrphanFilesLoadState.Success -> "Неприкреплённые файлы: ${state.files.size}"
            SettingsOrphanFilesLoadState.Loading -> "Неприкреплённые файлы"
            is SettingsOrphanFilesLoadState.Error -> "Неприкреплённые файлы"
        },
        subtitle = "Локальные файлы из каталога приложения, которых больше нет в таблице file.",
        headerActions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onRefresh) {
                    Text("Обновить")
                }
                if (state is SettingsOrphanFilesLoadState.Success && state.files.isNotEmpty()) {
                    Button(onClick = onDeleteAll) {
                        Text("Удалить все")
                    }
                }
            }
        }
    ) {
        when (state) {
            SettingsOrphanFilesLoadState.Loading -> LoadingUi()
            is SettingsOrphanFilesLoadState.Error -> ValidationErrorsCard(
                errorMessages = listOf(state.message)
            )
            is SettingsOrphanFilesLoadState.Success -> {
                if (state.files.isEmpty()) {
                    Text(
                        text = "Локальных orphan-файлов нет.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = state.files,
                            key = { it.path }
                        ) { file ->
                            SettingsOrphanFileRow(
                                file = file,
                                onOpen = { onOpen(file.path) },
                                onDelete = { onDelete(file.path) },
                            )
                        }
                    }
                }
            }
        }

        actionError?.let { message ->
            Spacer(Modifier.height(8.dp))
            ValidationErrorsCard(
                errorMessages = listOf(message),
                onErrorClick = { onDismissActionError() },
            )
        }
    }
}

@Composable
private fun SettingsOrphanFileRow(
    file: LocalOrphanFile,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Размер: ${file.sizeBytes.toReadableFileSize()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = file.relativePath,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen) {
                    Text("Открыть")
                }
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    )
                ) {
                    Text("Удалить локально")
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    headerActions: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (headerActions != null) {
                    Spacer(Modifier.width(12.dp))
                    headerActions()
                }
            }
            content()
        }
    }
}

private fun Long.toReadableFileSize(): String {
    val kiloByte = 1024L
    val megaByte = kiloByte * 1024
    return when {
        this >= megaByte -> String.format("%.1f MB", this.toDouble() / megaByte)
        this >= kiloByte -> String.format("%.1f KB", this.toDouble() / kiloByte)
        else -> "$this B"
    }
}
