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
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.SyncQueueStatus
import ru.pavlig43.datetime.getCurrentLocalDateTime

/**
 * Компонент шапки, который держит локальное состояние синхронизации для UI.
 *
 * Пока удаленная часть не подключена, он показывает состояние локальной очереди и дает
 * единое место, куда позже можно будет добавить запросы `status check` и реальный `push/pull`.
 */
class SyncComponent(
    componentContext: ComponentContext,
    private val syncQueueRepository: SyncQueueRepository,
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
            val pendingCount = syncQueueRepository.getChangesCount(SyncQueueStatus.PENDING)
            val failedCount = syncQueueRepository.getChangesCount(SyncQueueStatus.FAILED)
            _uiState.update {
                it.copy(
                    pendingChangesCount = pendingCount,
                    failedChangesCount = failedCount,
                    hasRemoteChanges = false,
                    isSyncRunning = false,
                    remoteSyncConfigured = false,
                    lastStatusCheckAt = getCurrentLocalDateTime(),
                )
            }
        }
    }

    /**
     * Действие по иконке синхронизации в шапке.
     *
     * Пока сервер еще не подключен, используем кнопку как ручное обновление локального статуса
     * и явно показываем, что удаленная синхронизация находится в разработке.
     */
    fun onSyncClick() {
        coroutineScope.launch {
            _uiState.update { it.copy(isSyncRunning = true) }
            val pendingCount = syncQueueRepository.getChangesCount(SyncQueueStatus.PENDING)
            val failedCount = syncQueueRepository.getChangesCount(SyncQueueStatus.FAILED)
            _uiState.update {
                it.copy(
                    pendingChangesCount = pendingCount,
                    failedChangesCount = failedCount,
                    hasRemoteChanges = false,
                    isSyncRunning = false,
                    remoteSyncConfigured = false,
                    lastStatusCheckAt = getCurrentLocalDateTime(),
                    lastError = null,
                )
            }
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
    val lastError: String? = null,
)

private const val STATUS_CHECK_INTERVAL_MILLIS = 3 * 60 * 1000L
