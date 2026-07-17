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
import ru.pavlig43.database.data.sync.SyncStatusSnapshot
import ru.pavlig43.database.data.sync.mirror.MirrorConflictWinner
import ru.pavlig43.database.data.sync.mirror.MirrorVersionConflict
import ru.pavlig43.doctor.api.DoctorDependencies
import ru.pavlig43.doctor.internal.component.DoctorOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorRemoteOrphanFilesLoadState
import ru.pavlig43.doctor.internal.component.DoctorStorageOverviewLoadState
import ru.pavlig43.doctor.internal.component.DoctorTool
import java.awt.Desktop
import java.io.File
import java.util.logging.Logger
import ru.pavlig43.files.api.PendingUpload

/**
 * Координирует диагностические инструменты Doctor и их защитные проверки.
 *
 * Компонент объединяет локальную диагностику файлов, сравнение S3 с активным
 * remote mirror и ручное разрешение конфликтов sync. Удаление объектов S3
 * разрешается лишь при доступном mirror, отсутствии локальных правок и пустом
 * реестре незавершённых загрузок.
 */
@Suppress("TooManyFunctions")
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

    private val _pendingUploads = MutableStateFlow<List<PendingUpload>>(emptyList())
    /** Незавершённые S3-загрузки, каждая из которых блокирует удалённую чистку. */
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
                    applyRemoteCleanupAvailability(syncStatus, _pendingUploads.value)
                }
        }
        coroutineScope.launch(Dispatchers.IO) {
            refreshRemoteCleanupAvailability()
        }
    }

    /**
     * Выбирает инструмент и обновляет его удалённые данные при открытии.
     *
     * S3-очистка заново проверяет защитные условия, а экран конфликтов запрашивает
     * свежий sync-статус.
     */
    fun selectTool(tool: DoctorTool) {
        _selectedTool.value = tool
        if (tool == DoctorTool.RemoteFileCleanup) {
            coroutineScope.launch(Dispatchers.IO) {
                refreshRemoteCleanupAvailability()
            }
        } else if (tool == DoctorTool.SyncConflicts) {
            coroutineScope.launch(Dispatchers.IO) { syncService.getStatus() }
        }
    }

    /**
     * Запускает безопасное разрешение конфликта на IO dispatcher.
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
            remoteFilesMaintenanceRepository
                .deleteRemoteFiles(currentState.files.mapTo(mutableSetOf()) { it.objectKey })
                .onFailure { throwable ->
                    _remoteOrphanFilesActionError.value =
                        throwable.message ?: "Не удалось удалить orphan-объекты S3."
                    return@launch
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
                val remoteKeys = remoteFilesMaintenanceRepository.getActiveMirrorObjectKeys().getOrThrow()
                val s3Keys = remoteFilesMaintenanceRepository.getS3ObjectKeys().getOrThrow()

                val onlyLocal = localKeys - remoteKeys
                val onlyRemote = remoteKeys - localKeys
                val onlyS3 = s3Keys - remoteKeys

                logger.info(
                    "Doctor S3 compare: " +
                        "local=${localKeys.size}, " +
                        "mirror=${remoteKeys.size}, " +
                        "s3=${s3Keys.size}, " +
                        "onlyLocal=${onlyLocal.size}, " +
                        "onlyMirror=${onlyRemote.size}, " +
                        "onlyS3=${onlyS3.size}"
                )
                logger.info("Doctor S3 compare local keys:\n${localKeys.toLogBlock()}")
                logger.info("Doctor S3 compare mirror keys:\n${remoteKeys.toLogBlock()}")
                logger.info("Doctor S3 compare S3 keys:\n${s3Keys.toLogBlock()}")
                logger.info("Doctor S3 compare only local:\n${onlyLocal.toLogBlock()}")
                logger.info("Doctor S3 compare only mirror:\n${onlyRemote.toLogBlock()}")
                logger.info("Doctor S3 compare only S3:\n${onlyS3.toLogBlock()}")
            }.onFailure { throwable ->
                _remoteOrphanFilesActionError.value =
                    throwable.message ?: "Не удалось сравнить локальную и удаленную базы по файлам."
                logger.warning("Doctor S3 compare failed: ${throwable.message}")
            }
        }
    }

    /**
     * Перечитывает реестр загрузок и статус mirror перед любой S3-операцией.
     *
     * Ошибка чтения реестра блокирует очистку: неизвестное состояние нельзя
     * трактовать как отсутствие незавершённых файлов.
     */
    private suspend fun refreshRemoteCleanupAvailability(): Boolean {
        val pending = remoteFilesMaintenanceRepository.getPendingUploads().getOrElse { throwable ->
            val message = throwable.message ?: "Не удалось прочитать реестр pending uploads."
            _remoteCleanupStatusMessage.value = message
            _isRemoteCleanupEnabled.value = false
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Error(message)
            return false
        }
        _pendingUploads.value = pending
        val syncStatus = syncService.getStatus()
        applyRemoteCleanupAvailability(syncStatus, pending)
        return remoteCleanupUnavailableMessage(syncStatus, pending) == null
    }

    /** Публикует единое состояние доступности кнопок удалённой очистки. */
    private fun applyRemoteCleanupAvailability(
        syncStatus: SyncStatusSnapshot,
        pendingUploads: List<PendingUpload>,
    ) {
        val unavailableMessage = remoteCleanupUnavailableMessage(syncStatus, pendingUploads)

        _isRemoteCleanupEnabled.value = unavailableMessage == null
        _remoteCleanupStatusMessage.value = unavailableMessage
            ?: "Синхронизация выполнена, можно запускать проверку S3."

        if (unavailableMessage != null) {
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Error(unavailableMessage)
        } else if (_remoteOrphanFilesState.value is DoctorRemoteOrphanFilesLoadState.Error) {
            _remoteOrphanFilesState.value = DoctorRemoteOrphanFilesLoadState.Idle
        }
    }

    /**
     * Возвращает первую причину, по которой S3-очистка должна быть заблокирована.
     *
     * Ошибка sync, локальные победители и незавершённые загрузки проверяются до
     * отметки последнего pull, чтобы UI показывал наиболее опасную причину.
     */
    @Suppress("ReturnCount")
    private fun remoteCleanupUnavailableMessage(
        syncStatus: SyncStatusSnapshot,
        pendingUploads: List<PendingUpload>,
    ): String? {
        if (syncStatus.remoteError != null) {
            return "Sync завершился с ошибкой: ${syncStatus.remoteError}"
        }
        if (!syncStatus.remoteSyncConfigured) {
            return "Remote sync не настроен."
        }
        if (syncStatus.pendingLocalChangesCount > 0) {
            return "Есть локальные изменения для отправки. Сначала выполните sync/push."
        }
        if (pendingUploads.isNotEmpty()) {
            return "Есть незавершённые загрузки файлов. Чистка S3 заблокирована."
        }
        if (syncStatus.lastPullAt == null) {
            return "Проверка S3 недоступна, пока приложение не синхронизировано."
        }
        if (syncStatus.hasRemoteChanges) {
            return "Есть неподтянутые remote-изменения. Сначала выполните sync/pull."
        }
        return null
    }

    private fun Collection<String>.toLogBlock(): String {
        return if (isEmpty()) {
            "<empty>"
        } else {
            sorted().joinToString(separator = "\n")
        }
    }
}
