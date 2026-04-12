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
import ru.pavlig43.database.data.sync.RemoteFileSyncState
import ru.pavlig43.database.data.sync.SyncStatusSnapshot
import ru.pavlig43.doctor.api.DoctorDependencies
import ru.pavlig43.doctor.internal.component.DoctorOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorRemoteOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorStorageOverviewLoadState
import ru.pavlig43.doctor.internal.component.DoctorTool
import java.awt.Desktop
import java.io.File
import java.util.logging.Logger

class DoctorComponent(
    componentContext: ComponentContext,
    dependencies: DoctorDependencies,
) : ComponentContext by componentContext, MainTabComponent {
    private val logger = Logger.getLogger("DoctorS3Compare")
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
        "Перед проверкой сначала синхронизируйте приложение."
    )
    val remoteCleanupStatusMessage = _remoteCleanupStatusMessage.asStateFlow()

    init {
        refreshStorageOverview()
        refreshOrphanFiles()
        coroutineScope.launch {
            syncService.status
                .filterNotNull()
                .collect { syncStatus ->
                    applyRemoteCleanupAvailability(syncStatus)
                }
        }
        coroutineScope.launch(Dispatchers.IO) {
            refreshRemoteCleanupAvailability()
        }
    }

    fun selectTool(tool: DoctorTool) {
        _selectedTool.value = tool
        if (tool == DoctorTool.RemoteFileCleanup) {
            coroutineScope.launch(Dispatchers.IO) {
                refreshRemoteCleanupAvailability()
            }
        }
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
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Loading
            if (!refreshRemoteCleanupAvailability()) {
                return@launch
            }
            remoteFilesMaintenanceRepository.getOrphanRemoteFiles()
                .onSuccess { files ->
                    _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Success(files)
                }
                .onFailure { throwable ->
                    _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Error(
                        throwable.message ?: "Не удалось загрузить orphan-объекты S3."
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
        coroutineScope.launch(Dispatchers.IO) {
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Loading
            if (!refreshRemoteCleanupAvailability()) {
                return@launch
            }
            remoteFilesMaintenanceRepository.deleteRemoteFile(objectKey)
                .onSuccess {
                    refreshRemoteOrphanFiles()
                }
                .onFailure { throwable ->
                    _remoteOrphanFilesActionError.value =
                        throwable.message ?: "Не удалось удалить объект из S3."
                }
        }
    }

    fun deleteAllRemoteOrphanFiles() {
        val currentState =
            remoteOrphanFilesState.value as? DoctorRemoteOrphanFilesLoadState.Success ?: return
        coroutineScope.launch(Dispatchers.IO) {
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Loading
            if (!refreshRemoteCleanupAvailability()) {
                return@launch
            }
            currentState.files.forEach { orphan ->
                remoteFilesMaintenanceRepository.deleteRemoteFile(orphan.objectKey)
                    .onFailure { throwable ->
                        _remoteOrphanFilesActionError.value =
                            throwable.message ?: "Не удалось удалить orphan-объекты S3."
                        return@launch
                    }
            }
            refreshRemoteOrphanFiles()
        }
    }

    fun dismissRemoteOrphanFilesActionError() {
        _remoteOrphanFilesActionError.value = null
    }

    fun logRemoteFileComparison() {
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val localKeys = remoteFilesMaintenanceRepository.getAttachedRemoteObjectKeys().getOrThrow()
                val remoteStates = syncService.loadCurrentRemoteFileStates().getOrThrow()
                val remoteKeys = remoteStates.currentRemoteObjectKeys()
                val remoteWithoutObjectKey = remoteStates.count { state ->
                    state.changeType != ru.pavlig43.database.data.sync.SyncChangeType.DELETE &&
                        state.deletedAt == null &&
                        state.remoteObjectKey.isNullOrBlank()
                }

                val onlyLocal = localKeys - remoteKeys
                val onlyRemote = remoteKeys - localKeys

                logger.info(
                    "Doctor S3 compare: " +
                        "local=${localKeys.size}, " +
                        "remote=${remoteKeys.size}, " +
                        "remoteWithoutObjectKey=$remoteWithoutObjectKey, " +
                        "onlyLocal=${onlyLocal.size}, " +
                        "onlyRemote=${onlyRemote.size}"
                )
                logger.info("Doctor S3 compare local keys:\n${localKeys.toLogBlock()}")
                logger.info("Doctor S3 compare remote keys:\n${remoteKeys.toLogBlock()}")
                logger.info("Doctor S3 compare only local:\n${onlyLocal.toLogBlock()}")
                logger.info("Doctor S3 compare only remote:\n${onlyRemote.toLogBlock()}")
            }.onFailure { throwable ->
                _remoteOrphanFilesActionError.value =
                    throwable.message ?: "Не удалось сравнить локальную и удаленную базы по файлам."
                logger.warning("Doctor S3 compare failed: ${throwable.message}")
            }
        }
    }

    private suspend fun refreshRemoteCleanupAvailability(): Boolean {
        val syncStatus = syncService.getStatus()
        applyRemoteCleanupAvailability(syncStatus)
        return remoteCleanupUnavailableMessage(syncStatus) == null
    }

    private fun applyRemoteCleanupAvailability(
        syncStatus: SyncStatusSnapshot,
    ) {
        val unavailableMessage = remoteCleanupUnavailableMessage(syncStatus)

        _isRemoteCleanupEnabled.value = unavailableMessage == null
        _remoteCleanupStatusMessage.value = unavailableMessage
            ?: "Синхронизация выполнена, можно запускать проверку S3."

        if (unavailableMessage != null) {
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Error(unavailableMessage)
        } else if (_remoteOrphanFilesState.value is DoctorRemoteOrphanFilesLoadState.Error) {
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Idle
        }
    }

    private fun remoteCleanupUnavailableMessage(
        syncStatus: SyncStatusSnapshot,
    ): String? {
        if (!syncStatus.remoteSyncConfigured) {
            return "Remote sync не настроен."
        }
        if (syncStatus.lastPullAt == null) {
            return "Проверка S3 недоступна, пока приложение не синхронизировано."
        }
        if (syncStatus.hasRemoteChanges) {
            return "Есть неподтянутые remote-изменения. Сначала выполните sync/pull."
        }
        return null
    }

    private fun List<RemoteFileSyncState>.currentRemoteObjectKeys(): Set<String> {
        return asSequence()
            .filter { it.changeType != ru.pavlig43.database.data.sync.SyncChangeType.DELETE }
            .filter { it.deletedAt == null }
            .mapNotNull { it.remoteObjectKey }
            .toSet()
    }

    private fun Collection<String>.toLogBlock(): String {
        return if (isEmpty()) {
            "<empty>"
        } else {
            sorted().joinToString(separator = "\n")
        }
    }
}
