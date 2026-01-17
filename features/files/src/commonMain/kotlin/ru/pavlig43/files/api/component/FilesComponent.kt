package ru.pavlig43.files.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.model.FileUi
import ru.pavlig43.files.api.uploadState.UploadState
import ru.pavlig43.files.internal.data.FilesRepository
import ru.pavlig43.files.internal.di.filesModule
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent

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

    internal fun calculateNocombroFileName(platformFile: PlatformFile): String {
        return "${ownerId}_${ownerType}_${platformFile.name}"
    }

    internal fun addNewFile(platformFile: PlatformFile) {
        val composeKey = _filesUi.value.maxOfOrNull { file -> file.composeKey }?.plus(1) ?: 0

        val newFile = FileUi(
            id = 0,
            platformFile = platformFile,
            composeKey = composeKey,
            uploadState = UploadState.Loading,
        )

        _filesUi.update { lst ->
            lst + newFile
        }

        coroutineScope.launch(Dispatchers.IO) {
            saveFileInNocombro(newFile)
        }
    }

    private suspend fun saveFileInNocombro(fileUi: FileUi) {
        val calculateNocombroFileName = calculateNocombroFileName(fileUi.platformFile)
        val nocombroFile = PlatformFile(FileKit.filesDir, calculateNocombroFileName)

        val result: Result<Unit> = runCatching {
            nocombroFile.write(fileUi.platformFile)
        }
        _filesUi.update { lst ->
            lst.map { file ->
                if (file.composeKey == fileUi.composeKey) {
                    file.copy(
                        platformFile = if (result.isSuccess) nocombroFile else fileUi.platformFile,
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
                path = fileUi.path,
                id = fileUi.id
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
        composeKey = composeKey,
        platformFile = PlatformFile(path),
        uploadState = UploadState.Success,
    )
}

