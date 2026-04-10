package ru.pavlig43.rootnocombro.internal.topbar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Badge
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

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
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    painter = painterResource(Res.drawable.menu),
                    contentDescription = "Menu",
                )
            }
        },
        actions = {
            SyncStatusButton(
                syncUiState = syncUiState,
                onSyncClick = syncComponent::onSyncClick,
                onPushClick = syncComponent::onPushClick,
                onPullClick = syncComponent::onPullClick,
                onRefreshClick = syncComponent::refreshStatus,
            )
            IconButton(onClick = settingsComponent::toggleDarkMode) {
                Icon(
                    painter = painterResource(
                        if (darkMode) Res.drawable.light_mode else Res.drawable.dark_mode
                    ),
                    contentDescription = "Toggle theme",
                )
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SyncStatusButton(
    syncUiState: SyncUiState,
    onSyncClick: () -> Unit,
    onPushClick: () -> Unit,
    onPullClick: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        ToolTipProject(tooltipText = buildSyncSummary(syncUiState)) {
            IconButton(
                onClick = { expanded = true },
                enabled = !syncUiState.isSyncRunning,
            ) {
                Box {
                    if (syncUiState.isSyncRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            painter = painterResource(syncIcon(syncUiState)),
                            contentDescription = "Sync status",
                            tint = syncTint(syncUiState)
                        )
                    }
                    if (
                        !syncUiState.isSyncRunning && (
                            syncUiState.failedChangesCount > 0 ||
                                syncUiState.pendingChangesCount > 0 ||
                                syncUiState.hasRemoteChanges
                            )
                    ) {
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
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        maxItemsInEachRow = 2,
                    ) {
                        TextButton(
                            modifier = Modifier.widthIn(min = 140.dp),
                            onClick = {
                                copySyncSnapshotToClipboard(syncUiState)
                            }
                        ) {
                            Text("Скопировать статус")
                        }
                        TextButton(
                            modifier = Modifier.widthIn(min = 140.dp),
                            enabled = !syncUiState.isSyncRunning,
                            onClick = {
                                onRefreshClick()
                            }
                        ) {
                            Text("Обновить статус")
                        }
                        TextButton(
                            modifier = Modifier.widthIn(min = 140.dp),
                            enabled = !syncUiState.isSyncRunning,
                            onClick = {
                                onPushClick()
                            }
                        ) {
                            Text("Отправить")
                        }
                        TextButton(
                            modifier = Modifier.widthIn(min = 140.dp),
                            enabled = !syncUiState.isSyncRunning,
                            onClick = {
                                onPullClick()
                            }
                        ) {
                            Text("Получить")
                        }
                        FilledTonalButton(
                            modifier = Modifier.widthIn(min = 140.dp),
                            enabled = !syncUiState.isSyncRunning,
                            onClick = {
                                onSyncClick()
                            }
                        ) {
                            Text("Синхронизировать все")
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
    return buildList {
        add(
            when {
                syncUiState.isSyncRunning -> syncUiState.runningActionLabel?.let { "$it..." } ?: "Синхронизация..."
                syncUiState.failedChangesCount > 0 -> "Есть ошибки синхронизации"
                syncUiState.hasRemoteChanges -> "На сервере есть новые изменения"
                syncUiState.pendingChangesCount > 0 -> "Есть локальные изменения для отправки"
                else -> "Локальная база синхронизирована"
            }
        )
        add(
            if (syncUiState.remoteSyncConfigured) {
                "Удаленная база подключена"
            } else {
                "Удаленная база пока не подключена"
            }
        )
        syncUiState.lastError?.let {
            add("Последняя ошибка: $it")
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
    }.joinToString(separator = "\n")
}

private fun buildSyncSummary(syncUiState: SyncUiState): String {
    return when {
        syncUiState.isSyncRunning -> syncUiState.runningActionLabel?.let { "$it..." } ?: "Синхронизация..."
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Синхронизация",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (syncUiState.isSyncRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
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
        syncUiState.lastPullAt?.let {
            Text(
                text = "Последнее получение: ${it.format(dateTimeFormat)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "Формат данных v${syncUiState.payloadVersion}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        syncUiState.lastRemoteCursor?.let {
            Text(
                text = "Курсор синхронизации: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun copySyncSnapshotToClipboard(
    syncUiState: SyncUiState,
) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val snapshot = buildString {
        appendLine("sync.remote_configured=${syncUiState.remoteSyncConfigured}")
        appendLine("sync.payload_version=${syncUiState.payloadVersion}")
        appendLine("sync.pending=${syncUiState.pendingChangesCount}")
        appendLine("sync.failed=${syncUiState.failedChangesCount}")
        appendLine("sync.has_remote_changes=${syncUiState.hasRemoteChanges}")
        appendLine("sync.last_sync_at=${syncUiState.lastSyncAt}")
        appendLine("sync.last_pull_at=${syncUiState.lastPullAt}")
        appendLine("sync.last_status_check_at=${syncUiState.lastStatusCheckAt}")
        appendLine("sync.last_remote_cursor=${syncUiState.lastRemoteCursor}")
        appendLine("sync.last_error=${syncUiState.lastError}")
    }.trimEnd()

    clipboard.setContents(StringSelection(snapshot), null)
}
