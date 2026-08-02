package ru.pavlig43.doctor.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.database.data.sync.mirror.MirrorConflictWinner
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.doctor.api.DoctorDependencies
import ru.pavlig43.doctor.internal.component.DoctorOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorRemoteOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorStorageOverviewLoadState
import ru.pavlig43.doctor.internal.component.DoctorTool
import ru.pavlig43.files.api.PendingUpload
import java.awt.Desktop
import java.io.File

/**
 * Координирует диагностические инструменты Doctor и их защитные проверки.
 *
 * Компонент объединяет локальную диагностику файлов, сравнение S3 с активным
 * удалённым зеркалом и ручное разрешение конфликтов синхронизации. Удаление объектов S3
 * разрешается лишь при доступном зеркале, отсутствии локальных правок и пустом
 * реестре незавершённых загрузок.
 */
@Suppress("TooManyFunctions")
class DoctorComponent(
    componentContext: ComponentContext,
    dependencies: DoctorDependencies,
) : ComponentContext by componentContext, MainTabComponent {
    private val coroutineScope = componentCoroutineScope()
    private val localFilesMaintenanceRepository = dependencies.localFilesMaintenanceRepository
    private val remoteFilesMaintenanceRepository = dependencies.remoteFilesMaintenanceRepository
    private val syncService = dependencies.syncService

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Доктор"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    private val _selectedTool = MutableStateFlow(DoctorTool.StorageOverview)
    val selectedTool = _selectedTool.asStateFlow()

    private val _storageOverviewState = MutableStateFlow<DoctorStorageOverviewLoadState>(
        DoctorStorageOverviewLoadState.Loading
    )
    val storageOverviewState = _storageOverviewState.asStateFlow()

    private val _orphanFilesState = MutableStateFlow<DoctorOrphanFilesLoadState>(
        DoctorOrphanFilesLoadState.Loading
    )
    val orphanFilesState = _orphanFilesState.asStateFlow()

    private val _orphanFilesActionError = MutableStateFlow<String?>(null)
    val orphanFilesActionError = _orphanFilesActionError.asStateFlow()

    private val _remoteOrphanFilesState = MutableStateFlow<DoctorRemoteOrphanFilesLoadState>(
        DoctorRemoteOrphanFilesLoadState.Idle
    )
    val remoteOrphanFilesState = _remoteOrphanFilesState.asStateFlow()

    private val _remoteOrphanFilesActionError = MutableStateFlow<String?>(null)
    val remoteOrphanFilesActionError = _remoteOrphanFilesActionError.asStateFlow()

    private val _isRemoteCleanupEnabled = MutableStateFlow(false)
    val isRemoteCleanupEnabled = _isRemoteCleanupEnabled.asStateFlow()

    private val _remoteCleanupStatusMessage = MutableStateFlow(
        "Нажмите «Обновить», чтобы сверить Room, YDB, загрузки и S3."
    )
    val remoteCleanupStatusMessage = _remoteCleanupStatusMessage.asStateFlow()

    private val _pendingUploads = MutableStateFlow<List<PendingUpload>>(emptyList())
    /** Старые незавершённые S3-загрузки, каждая из которых блокирует удалённую чистку. */
    val pendingUploads = _pendingUploads.asStateFlow()

    private val _syncConflicts = MutableStateFlow<List<MirrorVersionConflict>>(emptyList())
    /** Актуальные конфликты равных версий из последнего статуса синхронизации. */
    val syncConflicts = _syncConflicts.asStateFlow()

    private val _syncConflictActionError = MutableStateFlow<String?>(null)
    /** Ошибка последней попытки выбрать локальную или удалённую строку. */
    val syncConflictActionError = _syncConflictActionError.asStateFlow()

    init {
        refreshStorageOverview()
        refreshOrphanFiles()
        coroutineScope.launch {
            syncService.status
                .filterNotNull()
                .collect { syncStatus ->
                    _syncConflicts.value = syncStatus.conflicts
                }
        }
        coroutineScope.launch(Dispatchers.IO) {
            remoteFilesMaintenanceRepository.getPendingUploads()
                .onSuccess { uploads -> _pendingUploads.value = uploads }
                .onFailure { throwable ->
                    _remoteCleanupStatusMessage.value = throwable.message
                        ?: "Не удалось прочитать список зависших загрузок."
                }
        }
    }

    /** Смена инструмента не запускает удалённую проверку. */
    fun selectTool(tool: DoctorTool) {
        _selectedTool.value = tool
    }

    /**
     * Запускает безопасное разрешение конфликта в диспетчере ввода-вывода.
     *
     * @param conflict пара строк, показанная пользователю.
     * @param useLocal `true` для локального содержимого, `false` для удалённого.
     */
    fun resolveSyncConflict(conflict: MirrorVersionConflict, useLocal: Boolean) {
        coroutineScope.launch(Dispatchers.IO) {
            _syncConflictActionError.value = null
            syncService.resolveConflict(
                conflict = conflict,
                winner = if (useLocal) MirrorConflictWinner.LOCAL else MirrorConflictWinner.REMOTE,
            ).onFailure { throwable ->
                _syncConflictActionError.value = throwable.message ?: "Не удалось разрешить конфликт sync."
            }
        }
    }

    /** Скрывает ошибку действия, не меняя список конфликтов. */
    fun dismissSyncConflictActionError() {
        _syncConflictActionError.value = null
    }

    fun refreshOrphanFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            _orphanFilesState.value = DoctorOrphanFilesLoadState.Loading
            localFilesMaintenanceRepository.getOrphanLocalFiles()
                .onSuccess { files ->
                    _orphanFilesState.value = DoctorOrphanFilesLoadState.Success(files)
                }
                .onFailure { throwable ->
                    _orphanFilesState.value = DoctorOrphanFilesLoadState.Error(
                        throwable.message ?: "Не удалось загрузить orphan-файлы."
                    )
                }
        }
    }

    fun refreshStorageOverview() {
        coroutineScope.launch(Dispatchers.IO) {
            _storageOverviewState.value = DoctorStorageOverviewLoadState.Loading
            localFilesMaintenanceRepository.getStorageOverview()
                .onSuccess { overview ->
                    _storageOverviewState.value = DoctorStorageOverviewLoadState.Success(overview)
                }
                .onFailure { throwable ->
                    _storageOverviewState.value = DoctorStorageOverviewLoadState.Error(
                        throwable.message ?: "Не удалось загрузить обзор хранилища."
                    )
                }
        }
    }

    fun refreshRemoteOrphanFiles() {
        coroutineScope.launch(Dispatchers.IO) {
            _isRemoteCleanupEnabled.value = false
            _remoteCleanupStatusMessage.value = "Идёт сверка Room, YDB, загрузок и S3."
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Loading

            val pending = remoteFilesMaintenanceRepository.getPendingUploads()
                .getOrElse { throwable ->
                    val message = throwable.message
                        ?: "Не удалось прочитать список зависших загрузок."
                    blockRemoteCleanup(message)
                    return@launch
                }
            _pendingUploads.value = pending
            if (pending.isNotEmpty()) {
                blockRemoteCleanup(
                    "Есть зависшие загрузки. Снимите блокировки и снова нажмите «Обновить».",
                )
                return@launch
            }

            remoteFilesMaintenanceRepository.getOrphanRemoteFiles()
                .onSuccess { files ->
                    _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Success(files)
                    _isRemoteCleanupEnabled.value = true
                    _remoteCleanupStatusMessage.value =
                        "Сверка завершена. Перед удалением все источники будут прочитаны ещё раз."
                }
                .onFailure { throwable ->
                    blockRemoteCleanup(
                        throwable.message ?: "Не удалось сверить Room, YDB, загрузки и S3.",
                    )
                }
        }
    }

    fun openOrphanFile(path: String) {
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                Desktop.getDesktop().open(File(path))
            }.onFailure { throwable ->
                _orphanFilesActionError.value = throwable.message ?: "Не удалось открыть файл."
            }
        }
    }

    fun deleteOrphanFile(path: String) {
        coroutineScope.launch(Dispatchers.IO) {
            localFilesMaintenanceRepository.deleteLocalFile(path)
                .onSuccess {
                    refreshStorageOverview()
                    refreshOrphanFiles()
                }
                .onFailure { throwable ->
                    _orphanFilesActionError.value = throwable.message ?: "Не удалось удалить файл."
                }
        }
    }

    fun deleteAllOrphanFiles() {
        val currentState = orphanFilesState.value as? DoctorOrphanFilesLoadState.Success ?: return
        coroutineScope.launch(Dispatchers.IO) {
            currentState.files.forEach { orphan ->
                localFilesMaintenanceRepository.deleteLocalFile(orphan.path)
                    .onFailure { throwable ->
                        _orphanFilesActionError.value =
                            throwable.message ?: "Не удалось удалить orphan-файлы."
                        return@launch
                    }
            }
            refreshStorageOverview()
            refreshOrphanFiles()
        }
    }

    fun dismissOrphanFilesActionError() {
        _orphanFilesActionError.value = null
    }

    fun deleteRemoteOrphanFile(objectKey: String) {
        if (!_isRemoteCleanupEnabled.value) return
        val currentFiles = (remoteOrphanFilesState.value as? DoctorRemoteOrphanFilesLoadState.Success)
            ?.files ?: return
        coroutineScope.launch(Dispatchers.IO) {
            _isRemoteCleanupEnabled.value = false
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Loading
            _remoteCleanupStatusMessage.value = "Повторная сверка перед удалением."
            remoteFilesMaintenanceRepository.deleteRemoteFile(objectKey)
                .onSuccess {
                    _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Success(
                        currentFiles.filterNot { it.objectKey == objectKey }
                    )
                    _remoteCleanupStatusMessage.value =
                        "Объект удалён. Нажмите «Обновить» перед следующим удалением."
                }
                .onFailure { throwable ->
                    val message = throwable.message ?: "Не удалось удалить объект из S3."
                    _remoteOrphanFilesActionError.value = message
                    blockRemoteCleanup(message)
                }
        }
    }

    fun deleteAllRemoteOrphanFiles() {
        if (!_isRemoteCleanupEnabled.value) return
        val currentState =
            remoteOrphanFilesState.value as? DoctorRemoteOrphanFilesLoadState.Success ?: return
        coroutineScope.launch(Dispatchers.IO) {
            _isRemoteCleanupEnabled.value = false
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Loading
            _remoteCleanupStatusMessage.value = "Повторная сверка перед удалением."
            remoteFilesMaintenanceRepository
                .deleteRemoteFiles(currentState.files.mapTo(mutableSetOf()) { it.objectKey })
                .onFailure { throwable ->
                    val message = throwable.message ?: "Не удалось удалить объекты из S3."
                    _remoteOrphanFilesActionError.value = message
                    blockRemoteCleanup(message)
                    return@launch
                }
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Success(emptyList())
            _remoteCleanupStatusMessage.value =
                "Объекты удалены. Нажмите «Обновить» перед следующим удалением."
        }
    }

    fun dismissRemoteOrphanFilesActionError() {
        _remoteOrphanFilesActionError.value = null
    }

    /** Убирает только локальную запись. YDB и S3 не читаются. */
    fun releasePendingUpload(objectKey: String) {
        coroutineScope.launch(Dispatchers.IO) {
            _remoteOrphanFilesActionError.value = null
            remoteFilesMaintenanceRepository.releasePendingUpload(objectKey)
                .onSuccess {
                    _pendingUploads.value = _pendingUploads.value
                        .filterNot { it.objectKey == objectKey }
                    _isRemoteCleanupEnabled.value = false
                    _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Idle
                    _remoteCleanupStatusMessage.value =
                        "Блокировка снята локально. Объект остался в S3. Нажмите «Обновить» для сверки."
                }
                .onFailure { throwable ->
                    _remoteOrphanFilesActionError.value =
                        throwable.message ?: "Не удалось снять блокировку загрузки."
                }
        }
    }

    private fun blockRemoteCleanup(message: String) {
        _isRemoteCleanupEnabled.value = false
        _remoteCleanupStatusMessage.value = message
        _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Error(message)
    }
}
