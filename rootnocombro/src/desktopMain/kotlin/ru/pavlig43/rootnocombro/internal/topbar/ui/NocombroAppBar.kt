package ru.pavlig43.rootnocombro.internal.topbar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.datetime.format
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.tooltip.ToolTipProject
import ru.pavlig43.datetime.dateTimeFormat
import ru.pavlig43.rootnocombro.api.component.SettingsComponent
import ru.pavlig43.rootnocombro.api.component.SyncComponent
import ru.pavlig43.rootnocombro.api.component.SyncUiState
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.check
import ru.pavlig43.theme.cloud_download
import ru.pavlig43.theme.dark_mode
import ru.pavlig43.theme.light_mode
import ru.pavlig43.theme.menu
import ru.pavlig43.theme.refresh
import ru.pavlig43.theme.warning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NocombroAppBar(
    settingsComponent: SettingsComponent,
    syncComponent: SyncComponent,
    onOpenDrawer: () -> Unit,
) {
    val darkMode by settingsComponent.darkMode.collectAsState()
    val syncUiState by syncComponent.uiState.collectAsState()
    CenterAlignedTopAppBar(
        title = { Text(text = "Nocombro") },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer){
                Icon(painter = painterResource( Res.drawable.menu), contentDescription = "Menu")
            }
        },
        actions = {
            SyncStatusButton(
                syncUiState = syncUiState,
                onSyncClick = syncComponent::onSyncClick,
                onRefreshClick = syncComponent::refreshStatus,
            )
            IconButton(onClick = settingsComponent::toggleDarkMode) {
                Icon(
                    painter = painterResource(if (darkMode) Res.drawable.light_mode else Res.drawable.dark_mode),
                    contentDescription = "Toggle theme"
                )
            }
        }
    )
}

@Composable
private fun SyncStatusButton(
    syncUiState: SyncUiState,
    onSyncClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ToolTipProject(tooltipText = buildSyncSummary(syncUiState)) {
            IconButton(onClick = { expanded = true }) {
                Box {
                    Icon(
                        painter = painterResource(syncIcon(syncUiState)),
                        contentDescription = "Sync status",
                        tint = syncTint(syncUiState)
                    )
                    if (syncUiState.failedChangesCount > 0 || syncUiState.pendingChangesCount > 0 || syncUiState.hasRemoteChanges) {
                        Badge(
                            containerColor = when {
                                syncUiState.failedChangesCount > 0 -> MaterialTheme.colorScheme.error
                                syncUiState.hasRemoteChanges -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SyncDropdownContent(syncUiState = syncUiState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextButton(
                            onClick = {
                                expanded = false
                                onRefreshClick()
                            }
                        ) {
                            Text("Проверить")
                        }
                        FilledTonalButton(
                            onClick = {
                                expanded = false
                                onSyncClick()
                            }
                        ) {
                            Text("Синхронизировать")
                        }
                    }
                }
            }
        }
    }
}

private fun syncIcon(syncUiState: SyncUiState) = when {
    syncUiState.failedChangesCount > 0 -> Res.drawable.warning
    syncUiState.hasRemoteChanges -> Res.drawable.cloud_download
    syncUiState.pendingChangesCount > 0 -> Res.drawable.refresh
    else -> Res.drawable.check
}

@Composable
private fun syncTint(syncUiState: SyncUiState): Color = when {
    syncUiState.failedChangesCount > 0 -> MaterialTheme.colorScheme.error
    syncUiState.hasRemoteChanges -> MaterialTheme.colorScheme.tertiary
    syncUiState.pendingChangesCount > 0 -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun buildSyncTooltip(syncUiState: SyncUiState): String {
    val lines = buildList {
        add(
            when {
                syncUiState.failedChangesCount > 0 -> "Есть ошибки синхронизации"
                syncUiState.hasRemoteChanges -> "На сервере есть новые изменения"
                syncUiState.pendingChangesCount > 0 -> "Есть локальные изменения для отправки"
                else -> "Локальная база синхронизирована"
            }
        )
        if (!syncUiState.remoteSyncConfigured) {
            add("Удаленная синхронизация пока не подключена")
        }
        if (syncUiState.pendingChangesCount > 0) {
            add("Локальных изменений: ${syncUiState.pendingChangesCount}")
        }
        if (syncUiState.failedChangesCount > 0) {
            add("Ошибок очереди: ${syncUiState.failedChangesCount}")
        }
        syncUiState.lastStatusCheckAt?.let {
            add("Последняя проверка: ${it.format(dateTimeFormat)}")
        }
    }
    return lines.joinToString(separator = "\n")
}

private fun buildSyncSummary(syncUiState: SyncUiState): String {
    return when {
        syncUiState.failedChangesCount > 0 -> "Есть ошибки синхронизации"
        syncUiState.hasRemoteChanges -> "На сервере есть новые изменения"
        syncUiState.pendingChangesCount > 0 -> "Есть локальные изменения"
        else -> "Синхронизация"
    }
}

@Composable
private fun SyncDropdownContent(syncUiState: SyncUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Синхронизация",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = buildSyncTooltip(syncUiState),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        syncUiState.lastSyncAt?.let {
            Text(
                text = "Последняя синхронизация: ${it.format(dateTimeFormat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
