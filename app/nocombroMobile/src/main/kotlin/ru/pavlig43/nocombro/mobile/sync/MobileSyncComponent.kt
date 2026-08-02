package ru.pavlig43.nocombro.mobile.sync

import com.arkivanov.decompose.ComponentContext
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitOpenFileSettings
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

/**
 * Компонент Decompose для панели синхронизации Android и готового отчёта.
 */
class MobileSyncComponent(
    componentContext: ComponentContext,
    private val repository: MobileSyncRepository,
    private val stateStore: MobileSyncStateStore,
    private val reportStore: MobileSyncReportStore,
    private val fileProviderAuthority: String,
) : ComponentContext by componentContext {
    private val scope = componentCoroutineScope()
    private val actionMutex = Mutex()
    private val _uiState = MutableStateFlow(MobileSyncUiState())
    val uiState: StateFlow<MobileSyncUiState> = _uiState.asStateFlow()

    init {
        loadInitialStatus()
    }


    /** Читает сохранённый статус и запускает не более одной фоновой проверки в час. */
    private fun loadInitialStatus() = launchExclusive {
        _uiState.update {
            it.copy(
                running = true,
                runningLabel = "Загрузка состояния",
            )
        }
        val (cachedStatus, reportSnapshotAt) = withContext(Dispatchers.IO) {
            stateStore.loadCachedStatus() to reportStore.latestSnapshotAt()
        }
        if (cachedStatus == null) {
            _uiState.update { it.copy(reportSnapshotAt = reportSnapshotAt) }
        } else {
            applyCachedStatus(cachedStatus, reportSnapshotAt)
        }

        val shouldCheckRemote = withContext(Dispatchers.IO) {
            stateStore.tryRecordBackgroundAttempt(
                attemptAtEpochMillis = System.currentTimeMillis(),
                minIntervalMillis = BACKGROUND_STATUS_CHECK_INTERVAL_MILLIS,
            )
        }
        if (!shouldCheckRemote) {
            _uiState.update { current ->
                current.copy(
                    statusText = if (cachedStatus == null) "Проверка отложена" else current.statusText,
                    running = false,
                    runningLabel = null,
                )
            }
            return@launchExclusive
        }

        performAction(
            label = "Проверка",
            recordManualAttempt = false,
            writeReport = true,
            block = repository::check,
        )
    }

    /**
     * Сворачивает или раскрывает панель синхронизации в главном меню.
     */
    fun toggleExpanded() {
        _uiState.update { it.copy(expanded = !it.expanded) }
    }

    /**
     * Проверяет удалённый статус и считает расхождения.
     */
    fun check() = runAction("Проверка", writeReport = true) {
        repository.check()
    }

    /**
     * Отправляет локальные победившие строки в YDB/S3.
     */
    fun push() = runAction("Отправка") {
        repository.push()
    }

    /**
     * Получает удалённые победившие строки и недостающие S3-файлы.
     */
    fun pull() = runAction("Получение") {
        repository.pull()
    }

    /**
     * Запускает полную синхронизацию: сначала отправку, затем получение.
     */
    fun sync() = runAction("Синхронизация") {
        repository.sync()
    }

    /** Открывает готовый Markdown-файл без чтения Room, YDB или S3. */
    fun openLatestReport() = launchExclusive {
        _uiState.update {
            it.copy(
                running = true,
                runningLabel = "Открытие отчёта",
                error = null,
            )
        }
        val result = withContext(Dispatchers.IO) {
            reportStore.latestReport().mapCatching { report ->
                FileKit.openFileWithDefaultApplication(
                    file = PlatformFile(report.absolutePath),
                    openFileSettings = FileKitOpenFileSettings(
                        authority = fileProviderAuthority,
                    ),
                )
            }
        }
        _uiState.update {
            it.copy(
                running = false,
                runningLabel = null,
                error = result.exceptionOrNull()?.mobileSyncErrorMessage(
                    "Не удалось открыть отчёт",
                ),
            )
        }
    }

    private fun runAction(
        label: String,
        writeReport: Boolean = false,
        block: suspend () -> MobileSyncRunResult,
    ) {
        launchExclusive {
            performAction(
                label = label,
                recordManualAttempt = true,
                writeReport = writeReport,
                block = block,
            )
        }
    }

    private suspend fun performAction(
        label: String,
        recordManualAttempt: Boolean,
        writeReport: Boolean,
        block: suspend () -> MobileSyncRunResult,
    ) {
        _uiState.update {
            it.copy(
                running = true,
                runningLabel = label,
                error = null,
            )
        }
        val result = withContext(Dispatchers.IO) {
            if (recordManualAttempt) {
                stateStore.recordManualAttempt(System.currentTimeMillis())
            }
            block()
        }
        var error = result.error ?: result.status.error
        var reportSnapshotAt = _uiState.value.reportSnapshotAt
        if (writeReport && (error == null)) {
            result.preview?.let { preview ->
                val writeResult = withContext(Dispatchers.IO) {
                    reportStore.write(preview)
                }
                if (writeResult.isSuccess) {
                    reportSnapshotAt = preview.snapshotAt
                } else {
                    error = writeResult.exceptionOrNull()?.mobileSyncErrorMessage(
                        "Не удалось записать отчёт"
                    )
                }
            }
        }
        withContext(Dispatchers.IO) {
            stateStore.saveStatus(result.status, error)
        }
        _uiState.update {
            it.copy(
                configured = result.status.configured,
                localChanges = result.status.localChanges,
                remoteChanges = result.status.remoteChanges,
                statusText = if (error == null) result.status.toStatusText() else "Ошибка",
                error = error,
                running = false,
                runningLabel = null,
                lastPushAt = result.lastPushAt ?: it.lastPushAt,
                lastPullAt = result.lastPullAt ?: it.lastPullAt,
                reportSnapshotAt = reportSnapshotAt,
            )
        }
    }

    private fun applyCachedStatus(
        status: MobileCachedSyncStatus,
        reportSnapshotAt: LocalDateTime?,
    ) {
        _uiState.update {
            it.copy(
                configured = status.configured,
                localChanges = status.localChanges,
                remoteChanges = status.remoteChanges,
                statusText = status.toStatusText(),
                error = status.error,
                reportSnapshotAt = reportSnapshotAt,
            )
        }
    }

    /** Не ставит повторное нажатие в очередь, пока текущая операция не завершилась. */
    private fun launchExclusive(block: suspend () -> Unit) {
        scope.launch {
            if (!actionMutex.tryLock()) return@launch
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                val error = throwable.mobileSyncErrorMessage("Синхронизация прервана")
                _uiState.update { current ->
                    current.copy(
                        running = false,
                        runningLabel = null,
                        error = error,
                        statusText = "Ошибка",
                    )
                }
            } finally {
                _uiState.update { current ->
                    if (!current.running) current else {
                        current.copy(running = false, runningLabel = null)
                    }
                }
                actionMutex.unlock()
            }
        }
    }
}

/**
 * Состояние панели синхронизации в главном меню.
 */
data class MobileSyncUiState(
    val expanded: Boolean = false,
    val configured: Boolean = false,
    val localChanges: Int = 0,
    val remoteChanges: Int = 0,
    val statusText: String = "Проверка...",
    val error: String? = null,
    val running: Boolean = false,
    val runningLabel: String? = null,
    val lastPushAt: LocalDateTime? = null,
    val lastPullAt: LocalDateTime? = null,
    val reportSnapshotAt: LocalDateTime? = null,
)

/**
 * Сводит внутренний статус синхронизации к короткому тексту для меню.
 *
 * Ошибка имеет высший приоритет, затем отсутствие настроек и конфликт равных
 * версий. Счётчики используются лишь когда синхронизация доступна и конфликтов нет.
 */
internal fun MobileSyncStatus.toStatusText(): String {
    if (error != null) return "Ошибка"
    if (!configured) return "Не настроено"
    if (conflicts.isNotEmpty()) return MOBILE_SYNC_CONFLICT_HINT
    return if (localChanges == 0 && remoteChanges == 0) "Синхронизировано" else "Есть правки"
}


private fun MobileCachedSyncStatus.toStatusText(): String {
    if (error != null) return "Ошибка"
    if (!configured) return "Не настроено"
    if (hasConflicts) return MOBILE_SYNC_CONFLICT_HINT
    return if (localChanges == 0 && remoteChanges == 0) "Синхронизировано" else "Есть правки"
}

private const val BACKGROUND_STATUS_CHECK_INTERVAL_MILLIS = 60 * 60 * 1000L
