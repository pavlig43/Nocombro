package ru.pavlig43.nocombro.mobile.sync

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.componentCoroutineScope

/**
 * Decompose-компонент Android-панели синхронизации и экрана предпросмотра.
 */
class MobileSyncComponent(
    componentContext: ComponentContext,
    private val repository: MobileSyncRepository,
) : ComponentContext by componentContext {
    private val scope = componentCoroutineScope()
    private val _uiState = MutableStateFlow(MobileSyncUiState())
    val uiState: StateFlow<MobileSyncUiState> = _uiState.asStateFlow()

    private val _previewState = MutableStateFlow(MobileSyncPreviewUiState())
    val previewState: StateFlow<MobileSyncPreviewUiState> = _previewState.asStateFlow()

    init {
        check()
    }

    /**
     * Сворачивает или раскрывает sync-панель в главном меню.
     */
    fun toggleExpanded() {
        _uiState.update { it.copy(expanded = !it.expanded) }
    }

    /**
     * Проверяет удалённый статус и считает расхождения.
     */
    fun check() = runAction("Проверка") {
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
     * Запускает полный sync: push, затем pull.
     */
    fun sync() = runAction("Синхронизация") {
        repository.sync()
    }

    /**
     * Обновляет предпросмотр локальных и удалённых правок.
     */
    fun refreshPreview() {
        scope.launch {
            _previewState.update {
                it.copy(
                    loading = true,
                    error = null,
                )
            }
            val preview = withContext(Dispatchers.IO) {
                repository.preview()
            }
            _previewState.update {
                it.copy(
                    loading = false,
                    localChanges = preview.localChanges,
                    remoteChanges = preview.remoteChanges,
                    error = preview.error,
                )
            }
        }
    }

    private fun runAction(
        label: String,
        block: suspend () -> MobileSyncRunResult,
    ) {
        scope.launch {
            _uiState.update {
                it.copy(
                    running = true,
                    runningLabel = label,
                    error = null,
                )
            }
            val result = withContext(Dispatchers.IO) {
                block()
            }
            _uiState.update {
                it.copy(
                    configured = result.status.configured,
                    localChanges = result.status.localChanges,
                    remoteChanges = result.status.remoteChanges,
                    statusText = result.status.toStatusText(),
                    error = result.error ?: result.status.error,
                    running = false,
                    runningLabel = null,
                    lastPushAt = result.lastPushAt ?: it.lastPushAt,
                    lastPullAt = result.lastPullAt ?: it.lastPullAt,
                )
            }
        }
    }
}

/**
 * UI state sync-панели в главном меню.
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
)

/**
 * UI state экрана предпросмотра sync-расхождений.
 */
data class MobileSyncPreviewUiState(
    val loading: Boolean = false,
    val localChanges: List<MobileExperimentChangeGroup> = emptyList(),
    val remoteChanges: List<MobileExperimentChangeGroup> = emptyList(),
    val error: String? = null,
)

/**
 * Сводит сырой sync-статус к короткому тексту для меню.
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
