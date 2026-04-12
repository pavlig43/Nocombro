package ru.pavlig43.files.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.files.buildCanonicalFileKey
import ru.pavlig43.database.data.files.buildManagedLocalFilePath
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.model.FileUi
import ru.pavlig43.files.api.uploadState.UploadState
import ru.pavlig43.files.internal.data.FilesRepository
import ru.pavlig43.files.internal.di.filesModule
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import java.io.File

/**
 * Базовый tab-компонент для вложений в формах.
 *
 * Компонент по-прежнему работает как локальный file-manager для формы, но теперь дополнительно
 * умеет готовить remote object key и инициировать загрузку удалённой копии через репозиторий.
 *
 * Важный принцип здесь такой:
 * - UI и форма работают с `PlatformFile` и локальным состоянием;
 * - репозиторий занимается метаданными и удалённым storage;
 * - стабильный `syncId` файла создаётся сразу при добавлении вложения, чтобы не зависеть
 *   от локального `id` из Room.
 */
abstract class FilesComponent(
    componentContext: ComponentContext,
    private val ownerId: Int,
    private val ownerType: OwnerType,
    dependencies: FilesDependencies,
) : ComponentContext by componentContext, FormTabComponent {
    override val title: String = "Файлы"

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(filesModule(dependencies))
    private val filesRepository: FilesRepository = scope.get()

    private val coroutineScope = componentCoroutineScope()
    private val _fileMessage = MutableStateFlow<String?>(null)
    private val _downloadingComposeKeys = MutableStateFlow<Set<Int>>(emptySet())
    val fileMessage = _fileMessage.asStateFlow()
    val downloadingComposeKeys = _downloadingComposeKeys.asStateFlow()

    protected open fun manualUploadBlockedReason(
        fileName: String,
    ): String? = null

    /**
     * Имя локальной копии файла в каталоге приложения.
     *
     * Пока оно по-прежнему привязано к локальному владельцу формы, чтобы не ломать
     * существующую desktop-логику работы с файлами.
     */
    internal fun calculateNocombroFileName(platformFile: PlatformFile): String {
        return buildCanonicalFileKey(
            ownerType = ownerType,
            fileSyncId = "preview",
            originalName = platformFile.name,
        ).substringAfterLast('/')
    }

    /**
     * Строит стабильный object key для удалённого storage.
     *
     * Основа ключа - `syncId` самого файла, а не локальный `id`, поэтому ключ можно
     * использовать в будущей кросс-девайс синхронизации.
     */
    internal fun calculateRemoteObjectKey(
        syncId: String,
        platformFile: PlatformFile,
    ): String {
        return buildCanonicalFileKey(
            ownerType = ownerType,
            fileSyncId = syncId,
            originalName = platformFile.name,
        )
    }

    /**
     * Добавляет файл в UI-состояние и сразу резервирует для него стабильный `syncId`.
     */
    internal fun addNewFile(platformFile: PlatformFile) {
        manualUploadBlockedReason(platformFile.name)?.let { message ->
            _fileMessage.value = message
            return
        }

        val composeKey = _filesUi.value.maxOfOrNull { file -> file.composeKey }?.plus(1) ?: 0
        val syncId = defaultSyncId()

        val newFile = FileUi(
            id = 0,
            syncId = syncId,
            updatedAt = defaultUpdatedAt(),
            platformFile = platformFile,
            composeKey = composeKey,
            displayName = platformFile.name,
            uploadState = UploadState.Loading,
        )

        _filesUi.update { lst ->
            lst + newFile
        }

        coroutineScope.launch(Dispatchers.IO) {
            saveFileInNocombro(newFile)
        }
    }

    @Suppress("RedundantSuspendModifier")
    /**
     * Сохраняет локальную копию файла в каталоге приложения и, если доступен backend,
     * отправляет этот же файл в удалённый bucket.
     */
    private suspend fun saveFileInNocombro(fileUi: FileUi) {
        val remoteObjectKey = calculateRemoteObjectKey(fileUi.syncId, fileUi.platformFile)
        val nocombroFile = PlatformFile(
            buildManagedLocalFilePath(remoteObjectKey)
        )

        val result: Result<Unit> = runCatching {
            File(nocombroFile.path).parentFile?.mkdirs()
            nocombroFile.write(fileUi.platformFile)
            filesRepository.uploadRemoteCopy(
                objectKey = remoteObjectKey,
                localPath = nocombroFile.path,
            ).getOrThrow()
        }
        _filesUi.update { lst ->
            lst.map { file ->
                if (file.composeKey == fileUi.composeKey) {
                    file.copy(
                        platformFile = if (result.isSuccess) nocombroFile else fileUi.platformFile,
                        remoteObjectKey = if (result.isSuccess) remoteObjectKey else file.remoteObjectKey,
                        remoteStorageProvider = if (result.isSuccess) {
                            filesRepository.remoteProviderId()
                        } else {
                            file.remoteStorageProvider
                        },
                        uploadState = if (result.isSuccess) UploadState.Success else UploadState.Error(
                            message = result.exceptionOrNull()?.message ?: "unknown error"
                        )
                    )
                } else file
            }
        }

    }

    internal fun retryLoadFile(composeKey: Int) {
        val file = _filesUi.value.first { it.composeKey == composeKey }
            .copy(uploadState = UploadState.Loading)
        coroutineScope.launch {
            saveFileInNocombro(file)
        }

    }

    internal fun openFile(fileUi: FileUi) {
        coroutineScope.launch(Dispatchers.IO) {
            filesRepository.ensureLocalFileForOpen(
                localPath = fileUi.path,
                remoteObjectKey = fileUi.remoteObjectKey,
            ).onSuccess {
                FileKit.openFileWithDefaultApplication(
                    file = PlatformFile(fileUi.path),
                )
            }.onFailure { throwable ->
                _fileMessage.value = throwable.message ?: "Не удалось открыть файл"
            }
        }
    }

    internal fun downloadFile(composeKey: Int) {
        val fileUi = _filesUi.value.firstOrNull { it.composeKey == composeKey } ?: return
        coroutineScope.launch(Dispatchers.IO) {
            _downloadingComposeKeys.update { it + composeKey }
            filesRepository.ensureLocalFileForOpen(
                localPath = fileUi.path,
                remoteObjectKey = fileUi.remoteObjectKey,
            ).onFailure { throwable ->
                _fileMessage.value = throwable.message ?: "Не удалось скачать файл"
            }
            _downloadingComposeKeys.update { it - composeKey }
        }
    }

    internal fun downloadAllMissingFiles() {
        val filesForDownload = _filesUi.value.filter { fileUi ->
            fileUi.remoteObjectKey != null && !hasLocalFile(fileUi)
        }
        if (filesForDownload.isEmpty()) {
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            filesForDownload.forEach { fileUi ->
                _downloadingComposeKeys.update { it + fileUi.composeKey }
                filesRepository.ensureLocalFileForOpen(
                    localPath = fileUi.path,
                    remoteObjectKey = fileUi.remoteObjectKey,
                ).onFailure { throwable ->
                    _fileMessage.value = throwable.message ?: "Не удалось скачать файл"
                }
                _downloadingComposeKeys.update { it - fileUi.composeKey }
            }
        }
    }

    internal fun hasLocalFile(fileUi: FileUi): Boolean {
        return File(fileUi.path).exists()
    }

    internal fun dismissFileMessage() {
        _fileMessage.value = null
    }


    internal fun removeFile(composeKey: Int) {

        _filesUi.update { lst ->
            lst.mapNotNull { file ->
                if (file.composeKey == composeKey) null
                else file
            }
        }


    }

    private val _filesUi = MutableStateFlow<List<FileUi>>(emptyList())
    val filesUi = _filesUi.asStateFlow()
    internal val initDataComponent: LoadInitDataComponent<List<FileUi>> =
        LoadInitDataComponent<List<FileUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { filesRepository.getInit(ownerId, ownerType).map { it.toListFileUi() } },
            onSuccessGetInitData = { files -> _filesUi.update { files } }
        )

    override suspend fun refreshDataAfterUpsert() {
        initDataComponent.retryLoadInitData()
    }

    protected val isAllFilesUpload: Flow<Boolean> =
        _filesUi.map { it.all { file -> file.uploadState is UploadState.Success } }

    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value?.toDto()
        val new = _filesUi.value.toDto()
        return filesRepository.update(ChangeSet(old, new))
    }

    private fun List<FileUi>.toDto(): List<FileBD> {
        return this.map { fileUi ->
            FileBD(
                ownerId = ownerId,
                ownerFileType = ownerType,
                displayName = fileUi.displayName,
                path = fileUi.path,
                remoteObjectKey = fileUi.remoteObjectKey,
                remoteStorageProvider = fileUi.remoteStorageProvider,
                id = fileUi.id,
                syncId = fileUi.syncId,
                updatedAt = fileUi.updatedAt,
            )
        }
    }
}

private fun List<FileBD>.toListFileUi(): List<FileUi> {
    return this.mapIndexed { ind, file -> file.toFileUI(ind) }
}


private fun FileBD.toFileUI(composeKey: Int): FileUi {
    return FileUi(
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        composeKey = composeKey,
        displayName = displayName,
        platformFile = PlatformFile(path),
        uploadState = UploadState.Success,
        remoteObjectKey = remoteObjectKey,
        remoteStorageProvider = remoteStorageProvider,
    )
}

