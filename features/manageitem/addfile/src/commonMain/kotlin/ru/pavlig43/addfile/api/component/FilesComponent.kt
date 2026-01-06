package ru.pavlig43.addfile.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import io.github.vinceglb.filekit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.addfile.api.data.RemoveState
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.FileData
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent


class FilesComponent<Files : List<FileData>>(
    componentContext: ComponentContext,
    private val getInitData: suspend () -> Result<Files>,
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()

    fun addPlatformPath(platformFile: PlatformFile) {
        val composeKey = _filesUi.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val newFile = FileUi(
            id = 0,
            platformFile = platformFile,
            composeKey = composeKey,
            uploadState = UploadState.Loading,
            removeState = RemoveState.Init
        )
        coroutineScope.launch(Dispatchers.IO) {
            saveFileInNocombro(newFile)
        }
    }

    private suspend fun saveFileInNocombro(platformFile: FileUi) {
        val nocombroFile = PlatformFile(FileKit.filesDir, platformFile.platformFile.name)

        _filesUi.update { lst ->
            lst + platformFile
        }

        val result: Result<Unit> = runCatching {
            nocombroFile.write(platformFile.platformFile)
        }
        _filesUi.update { lst ->
            lst.map { file ->
                if (file.composeKey == platformFile.composeKey) {
                    file.copy(
                        platformFile = if (result.isSuccess) nocombroFile else platformFile.platformFile,
                        uploadState = if (result.isSuccess) UploadState.Success else UploadState.Error(
                            message = result.exceptionOrNull()?.message ?: "unknown error"
                        )
                    )
                } else file
            }
        }

    }

    fun retryLoadFile(composeKey: Int) {
        val file = _filesUi.value.first { it.composeKey == composeKey }
            .copy(uploadState = UploadState.Loading)
        coroutineScope.launch {
            saveFileInNocombro(file)
        }

    }


    fun removeFile(composeKey: Int) {
        coroutineScope.launch {
            val fileForRemove = _filesUi.value.first { it.composeKey == composeKey }.copy(removeState = RemoveState.InProgress)
            _filesUi.update { lst->
                lst.map { file->
                    if (file.composeKey == fileForRemove.composeKey) fileForRemove
                    else file
                }
            }

            _filesUi.update { lst ->
                lst.mapNotNull { file ->
                    when {
                        file.composeKey != fileForRemove.composeKey -> file
                        file.uploadState is UploadState.Error -> null
                        file.uploadState is UploadState.Success -> {
                            runCatching {
                                fileForRemove.platformFile.delete(false)
                            }.fold(
                                onSuccess = { null },
                                onFailure = {
                                    file.copy(
                                        removeState = RemoveState.Error(
                                            it.message ?: "Unknown"
                                        )
                                    )
                                }
                            )
                        }

                        else -> file
                    }

                }
            }

        }

    }

    private val _filesUi = MutableStateFlow<List<FileUi>>(emptyList())
    val filesUi = _filesUi.asStateFlow()
    val loadInitDataComponent: LoadInitDataComponent<List<FileUi>> =
        LoadInitDataComponent<List<FileUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().map { it.toListFileUi() } },
            onSuccessGetInitData = { files -> _filesUi.update { files } }
        )


    val isAllFilesUpload: Flow<Boolean> =
        _filesUi.map { it.all { file -> file.uploadState is UploadState.Success && file.removeState is RemoveState.Init } }


}

private fun List<FileData>.toListFileUi(): List<FileUi> {
    return this.mapIndexed { ind, file -> file.toFileUI(ind) }
}

private fun FileData.toFileUI(composeKey: Int): FileUi {
    return FileUi(
        id = id,
        composeKey = composeKey,
        platformFile = PlatformFile(path),
        uploadState = UploadState.Success,
        removeState = RemoveState.Init
    )
}





