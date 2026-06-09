package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadRepository
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadSummary
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncStatusSnapshot

/**
 * Компонент шапки, который держит локальное состояние синхронизации для UI.
 *
 * Компонент не знает деталей конкретного удаленного источника и работает через
 * `SyncService` и вспомогательные репозитории.
 */
class SyncComponent(
    componentContext: ComponentContext,
    private val syncService: SyncService,
    private val remoteFileBatchDownloadRepository: RemoteFileBatchDownloadRepository,
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()
    // В этом компоненте есть несколько конкурирующих действий:
    // ручной pull/push/sync и периодический refreshStatus().
    // Без общей блокировки они могут выполняться параллельно и перетирать друг другу
    // uiState: например, refresh завершится посередине pull и вернет старый статус в UI.
    private val syncActionMutex = Mutex()

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
            // Mutex здесь нужен не для потокобезопасности StateFlow как такового,
            // а чтобы логически сериализовать sync-операции для UI.
            syncActionMutex.withLock {
                _uiState.update {
                    it.copy(
                        isSyncRunning = true,
                        runningActionLabel = "Проверка",
                        lastError = null,
                    )
                }
                val status = withContext(Dispatchers.IO) {
                    syncService.getStatus()
                }
                updateUiState(status, isSyncRunning = false, lastError = null)
            }
        }
    }

    /**
     * Действие по иконке синхронизации в шапке.
     */
    fun onSyncClick() {
        coroutineScope.launch {
            syncActionMutex.withLock {
                _uiState.update {
                    it.copy(
                        isSyncRunning = true,
                        runningActionLabel = "Синхронизация",
                        lastError = null,
                    )
                }
                val result = withContext(Dispatchers.IO) {
                    syncService.syncOnce()
                }
                updateUiState(
                    status = result.status,
                    isSyncRunning = false,
                    lastError = result.error,
                    lastSyncAt = result.lastSyncAt,
                    lastPullAt = result.lastPullAt,
                )
            }
        }
    }

    fun onPushClick() {
        coroutineScope.launch {
            syncActionMutex.withLock {
                _uiState.update {
                    it.copy(
                        isSyncRunning = true,
                        runningActionLabel = "Отправка",
                        lastError = null,
                    )
                }
                val result = withContext(Dispatchers.IO) {
                    syncService.pushOnce()
                }
                updateUiState(
                    status = result.status,
                    isSyncRunning = false,
                    lastError = result.error,
                    lastSyncAt = result.lastSyncAt,
                    lastPullAt = result.lastPullAt,
                )
            }
        }
    }

    /**
     * Выполняет безопасный сценарий "получить и файлы":
     *
     * 1. Сначала подтягивает метаданные из удаленной БД через обычный `pull`.
     * 2. Если `pull` прошел успешно, догружает отсутствующие локальные копии файлов из S3.
     *
     * Такой порядок нужен, чтобы скачивание файлов шло только по тем записям `file`,
     * которые уже появились в локальной БД после синхронизации метаданных.
     */
    fun onPullClick() {
        coroutineScope.launch {
            // Весь сценарий pull -> download files должен быть для UI одной операцией.
            // Если в середине вклинится periodic refresh, пользователь увидит
            // скачущий статус, старые timestamps или потерю текста текущего шага.
            syncActionMutex.withLock {
                _uiState.update {
                    it.copy(
                        isSyncRunning = true,
                        runningActionLabel = "Получение",
                        lastError = null,
                        lastFilesDownloadSummary = null,
                    )
                }
                val result = withContext(Dispatchers.IO) {
                    syncService.pullOnce()
                }
                if (result.error != null) {
                    updateUiState(
                        status = result.status,
                        isSyncRunning = false,
                        lastError = result.error,
                        lastSyncAt = result.lastSyncAt,
                        lastPullAt = result.lastPullAt,
                    )
                    return@withLock
                }

                _uiState.update {
                    it.copy(
                        isSyncRunning = true,
                        runningActionLabel = "Подгрузка файлов",
                    )
                }
                val downloadResult = withContext(Dispatchers.IO) {
                    remoteFileBatchDownloadRepository.downloadMissingLocalCopies()
                }
                updateUiState(
                    status = result.status,
                    isSyncRunning = false,
                    lastError = downloadResult.exceptionOrNull()?.message,
                    lastSyncAt = result.lastSyncAt,
                    lastPullAt = result.lastPullAt,
                    lastFilesDownloadSummary = downloadResult.getOrNull()?.toUiSummary(),
                )
            }
        }
    }

    private fun updateUiState(
        status: SyncStatusSnapshot,
        isSyncRunning: Boolean,
        lastError: String?,
        lastSyncAt: LocalDateTime? = null,
        lastPullAt: LocalDateTime? = null,
        lastFilesDownloadSummary: String? = null,
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
                lastPullAt = lastPullAt ?: status.lastPullAt,
                lastRemoteCursor = status.lastRemoteCursor,
                payloadVersion = status.payloadVersion,
                lastError = lastError ?: status.remoteError,
                lastFilesDownloadSummary = lastFilesDownloadSummary ?: it.lastFilesDownloadSummary,
                runningActionLabel = null,
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
    val lastFilesDownloadSummary: String? = null,
    val runningActionLabel: String? = null,
)

private const val STATUS_CHECK_INTERVAL_MILLIS = 3 * 60 * 1000L

/**
 * Сводит технический результат массовой догрузки файлов к короткой строке для UI.
 */
private fun RemoteFileBatchDownloadSummary.toUiSummary(): String {
    return when {
        scannedCount == 0 -> "Все удаленные файлы уже есть локально."
        failedCount == 0 -> "Подгружено файлов: $downloadedCount из $scannedCount."
        downloadedCount == 0 -> "Не удалось подгрузить ни одного файла. Ошибок: $failedCount."
        else -> "Подгружено файлов: $downloadedCount из $scannedCount. Ошибок: $failedCount."
    }
}
