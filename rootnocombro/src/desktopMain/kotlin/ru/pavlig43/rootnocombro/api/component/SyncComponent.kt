package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncStatusSnapshot

/**
 * Компонент шапки, который держит локальное состояние синхронизации для UI.
 *
 * Компонент не знает деталей конкретного backend и работает через sync-service.
 */
class SyncComponent(
    componentContext: ComponentContext,
    private val syncService: SyncService,
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        refreshStatus()
        startPeriodicStatusCheck()
    }

    /**
     * Обновляет локальный статус синхронизации.
     */
    fun refreshStatus() {
        coroutineScope.launch {
            updateUiState(syncService.getStatus(), isSyncRunning = false, lastError = null)
        }
    }

    /**
     * Действие по иконке синхронизации в шапке.
     */
    fun onSyncClick() {
        coroutineScope.launch {
            _uiState.update { it.copy(isSyncRunning = true, lastError = null) }
            val result = syncService.syncOnce()
            updateUiState(
                status = result.status,
                isSyncRunning = false,
                lastError = result.error,
                lastSyncAt = result.lastSyncAt,
            )
        }
    }

    private fun updateUiState(
        status: SyncStatusSnapshot,
        isSyncRunning: Boolean,
        lastError: String?,
        lastSyncAt: LocalDateTime? = null,
    ) {
        _uiState.update {
            it.copy(
                pendingChangesCount = status.pendingChangesCount,
                failedChangesCount = status.failedChangesCount,
                hasRemoteChanges = status.hasRemoteChanges,
                isSyncRunning = isSyncRunning,
                remoteSyncConfigured = status.remoteSyncConfigured,
                lastStatusCheckAt = status.lastStatusCheckAt,
                lastSyncAt = lastSyncAt ?: status.lastSyncAt,
                lastPullAt = status.lastPullAt,
                lastRemoteCursor = status.lastRemoteCursor,
                payloadVersion = status.payloadVersion,
                lastError = lastError,
            )
        }
    }

    /**
     * Периодически обновляет локальный статус синхронизации, пока жив компонент.
     */
    private fun startPeriodicStatusCheck() {
        coroutineScope.launch {
            while (isActive) {
                delay(STATUS_CHECK_INTERVAL_MILLIS)
                refreshStatus()
            }
        }
    }
}

data class SyncUiState(
    val pendingChangesCount: Int = 0,
    val failedChangesCount: Int = 0,
    val hasRemoteChanges: Boolean = false,
    val isSyncRunning: Boolean = false,
    val remoteSyncConfigured: Boolean = false,
    val lastStatusCheckAt: LocalDateTime? = null,
    val lastSyncAt: LocalDateTime? = null,
    val lastPullAt: LocalDateTime? = null,
    val lastRemoteCursor: String? = null,
    val payloadVersion: Int = 0,
    val lastError: String? = null,
)

private const val STATUS_CHECK_INTERVAL_MILLIS = 3 * 60 * 1000L
