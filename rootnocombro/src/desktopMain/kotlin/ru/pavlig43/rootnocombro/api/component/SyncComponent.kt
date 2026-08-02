package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.ComponentContext
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.database.data.files.remote.RemoteFileBatchDownloadSummary
import ru.pavlig43.database.data.sync.SyncService
import ru.pavlig43.database.data.sync.SyncStatusSnapshot
import ru.pavlig43.datastore.SyncCheckAttemptStore

/**
 * Компонент шапки, который хранит локальное состояние синхронизации для интерфейса.
 *
 * Компонент не знает деталей конкретного удаленного источника и работает через
 * `SyncService` и вспомогательные репозитории.
 */
class SyncComponent(
    componentContext: ComponentContext,
    private val syncService: SyncService,
    private val syncCheckAttemptStore: SyncCheckAttemptStore,
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()
    // Одна блокировка не даёт ручным действиям и стартовой проверке
    // выполняться параллельно и перетирать состояние UI.
    private val syncActionMutex = Mutex()

    private val _uiState = MutableStateFlow(SyncUiState())
    val uiState: StateFlow<SyncUiState> = _uiState.asStateFlow()

    init {
        loadInitialStatus()
    }

    /** Сначала читает локальный `sync_state`, затем при нужде один раз проверяет YDB. */
    private fun loadInitialStatus() = launchExclusive {
        beginAction("Загрузка состояния")
        val localStatus = withContext(Dispatchers.IO) { syncService.getLocalStatus() }
        updateUiState(localStatus, isSyncRunning = true, lastError = null)

        val shouldCheckRemote = withContext(Dispatchers.IO) {
            syncCheckAttemptStore.tryRecordBackgroundAttempt(
                attemptAtEpochMillis = System.currentTimeMillis(),
                minIntervalMillis = BACKGROUND_STATUS_CHECK_INTERVAL_MILLIS,
            )
        }
        if (!shouldCheckRemote) {
            _uiState.update { current ->
                current.copy(isSyncRunning = false, runningActionLabel = null)
            }
            return@launchExclusive
        }

        beginAction("Проверка")
        val remoteStatus = withContext(Dispatchers.IO) { syncService.getStatus() }
        updateUiState(remoteStatus, isSyncRunning = false, lastError = null)
    }

    /** Ручная проверка обходит часовой лимит. */
    fun refreshStatus() = launchExclusive {
        beginAction("Проверка")
        val status = withContext(Dispatchers.IO) {
            syncCheckAttemptStore.recordManualAttempt(System.currentTimeMillis())
            syncService.getStatus()
        }
        updateUiState(status, isSyncRunning = false, lastError = null)
    }

    /**
     * Действие по иконке синхронизации в шапке.
     */
    fun onSyncClick() = launchExclusive {
        beginAction("Синхронизация")
        val result = withContext(Dispatchers.IO) {
            syncCheckAttemptStore.recordManualAttempt(System.currentTimeMillis())
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

    fun onPushClick() = launchExclusive {
        beginAction("Отправка")
        val result = withContext(Dispatchers.IO) {
            syncCheckAttemptStore.recordManualAttempt(System.currentTimeMillis())
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

    /** Открывает готовый отчёт с диска и не читает Room или YDB. */
    fun onCreateReportClick() = launchExclusive {
        beginAction("Открытие отчёта")
        val result = withContext(Dispatchers.IO) {
            syncService.createSyncAnalysisReport().mapCatching { file ->
                FileKit.openFileWithDefaultApplication(PlatformFile(file))
                file
            }
        }
        _uiState.update {
            it.copy(
                isSyncRunning = false,
                runningActionLabel = null,
                lastError = result.exceptionOrNull()?.message,
            )
        }
    }

    /**
     * Выполняет безопасный сценарий "получить и файлы":
     *
     * 1. Сначала получает метаданные из удалённой БД.
     * 2. После успешного получения догружает отсутствующие локальные копии файлов из S3.
     *
     * Такой порядок нужен, чтобы скачивание файлов шло только по тем записям `file`,
     * которые уже появились в локальной БД после синхронизации метаданных.
     */
    fun onPullClick() = launchExclusive {
        beginAction("Получение", clearFilesSummary = true)
        val result = withContext(Dispatchers.IO) {
            syncCheckAttemptStore.recordManualAttempt(System.currentTimeMillis())
            syncService.pullOnce()
        }
        updateUiState(
            status = result.status,
            isSyncRunning = false,
            lastError = result.error,
            lastSyncAt = result.lastSyncAt,
            lastPullAt = result.lastPullAt,
            lastFilesDownloadSummary = result.filesDownloadSummary?.toUiSummary(),
        )
    }

    @Suppress("LongParameterList")
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
                pendingLocalChangesCount = status.pendingLocalChangesCount,
                remoteChangesCount = status.remoteChangesCount,
                hasRemoteChanges = status.hasRemoteChanges,
                isSyncRunning = isSyncRunning,
                remoteSyncConfigured = status.remoteSyncConfigured,
                lastStatusCheckAt = status.lastStatusCheckAt,
                reportSnapshotAt = status.reportSnapshotAt,
                lastSyncAt = lastSyncAt ?: status.lastSyncAt,
                lastPullAt = lastPullAt ?: status.lastPullAt,
                lastError = lastError ?: status.remoteError,
                lastFilesDownloadSummary = lastFilesDownloadSummary ?: it.lastFilesDownloadSummary,
                runningActionLabel = null,
            )
        }
    }

    private fun beginAction(
        label: String,
        clearFilesSummary: Boolean = false,
    ) {
        _uiState.update { current ->
            current.copy(
                isSyncRunning = true,
                runningActionLabel = label,
                lastError = null,
                lastFilesDownloadSummary = if (clearFilesSummary) {
                    null
                } else {
                    current.lastFilesDownloadSummary
                },
            )
        }
    }

    /** Не ставит повторное нажатие в очередь, пока текущая операция не завершилась. */
    private fun launchExclusive(block: suspend () -> Unit) {
        coroutineScope.launch {
            if (!syncActionMutex.tryLock()) return@launch
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                _uiState.update { current ->
                    current.copy(
                        isSyncRunning = false,
                        runningActionLabel = null,
                        lastError = throwable.message
                            ?: "Операция синхронизации завершилась с ошибкой.",
                    )
                }
            } finally {
                _uiState.update { current ->
                    if (!current.isSyncRunning) current else {
                        current.copy(isSyncRunning = false, runningActionLabel = null)
                    }
                }
                syncActionMutex.unlock()
            }
        }
    }
}

data class SyncUiState(
    val pendingLocalChangesCount: Int = 0,
    val remoteChangesCount: Int = 0,
    val hasRemoteChanges: Boolean = false,
    val isSyncRunning: Boolean = false,
    val remoteSyncConfigured: Boolean = false,
    val lastStatusCheckAt: LocalDateTime? = null,
    val reportSnapshotAt: LocalDateTime? = null,
    val lastSyncAt: LocalDateTime? = null,
    val lastPullAt: LocalDateTime? = null,
    val lastError: String? = null,
    val lastFilesDownloadSummary: String? = null,
    val runningActionLabel: String? = null,
)

// Фоновая проверка бывает только при запуске и не чаще одного раза в час.
private const val BACKGROUND_STATUS_CHECK_INTERVAL_MILLIS = 60 * 60 * 1000L

/**
 * Сводит технический результат массовой догрузки файлов к короткой строке для интерфейса.
 */
private fun RemoteFileBatchDownloadSummary.toUiSummary(): String {
    return when {
        scannedCount == 0 -> "Все удаленные файлы уже есть локально."
        failedCount == 0 -> "Подгружено файлов: $downloadedCount из $scannedCount."
        downloadedCount == 0 -> "Не удалось подгрузить ни одного файла. Ошибок: $failedCount."
        else -> "Подгружено файлов: $downloadedCount из $scannedCount. Ошибок: $failedCount."
    }
}
