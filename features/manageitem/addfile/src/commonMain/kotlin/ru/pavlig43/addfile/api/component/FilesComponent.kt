package ru.pavlig43.addfile.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.FileData
import ru.pavlig43.core.mapTo
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent


class FilesComponent<Files : List<FileData>>(
    componentContext: ComponentContext,
    private val getInitData: suspend () -> RequestResult<Files>,
) : ComponentContext by componentContext, IFilesComponent {

    private val coroutineScope = componentCoroutineScope()

    override fun addFilePath(path: String,) {
        val addedIndex = _filesUi.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val startAddedFile = FileUi(
            id = 0,
            platformFile = PlatformFile(path),
            composeKey = addedIndex,
            uploadState = UploadState.Loading
        )
        coroutineScope.launch(Dispatchers.IO) {
            loadFile(startAddedFile)
        }
    }

    //TODO сделать для телефона fileExeption
    // Failure(
    // java.io.FileNotFoundException:
    // content:/com.android.providers.media.documents/document/image%3A1000000034:
    // open failed: ENOENT (No such file or directory))
    private suspend fun loadFile(fileUi: FileUi) {
        val innerFile = PlatformFile(FileKit.filesDir, fileUi.platformFile.name)

        updateList {
            it.add(fileUi.copy(uploadState = UploadState.Loading))
            it
        }

        val result: Result<Unit> = runCatching {
            innerFile.write(fileUi.platformFile)
        }
        val updatedFile = _filesUi.value.find { it.composeKey == fileUi.composeKey }?.copy(
            platformFile = if (result.isSuccess) innerFile else fileUi.platformFile,
            uploadState = if (result.isSuccess) UploadState.Success else UploadState.Error(
                message = result.exceptionOrNull()?.message ?: "unknown error"
            )
        )
        updatedFile?.let { file ->
            updateList { lst ->
                lst[fileUi.composeKey] = file
                lst
            }
        }

    }

    override fun retryLoadFile(index: Int) {
        val file = _filesUi.value[index]
        updateList { lst ->
            lst.removeAt(index)
            lst
        }

        coroutineScope.launch(Dispatchers.IO) {
            loadFile(file)
        }

    }


    private fun updateList(updateAction: (MutableList<FileUi>) -> List<FileUi>) {
        val updatedFiles = _filesUi.value.toMutableList()
        _filesUi.update { updateAction(updatedFiles) }
    }


    override fun removeFile(index: Int) {
        updateList { lst ->
            lst.removeIf { it.composeKey == index }
            lst
        }
    }

    private val _filesUi = MutableStateFlow<List<FileUi>>(emptyList())
    override val filesUi = _filesUi.asStateFlow()
    override val loadInitDataComponent: LoadInitDataComponent<List<FileUi>> =
        LoadInitDataComponent<List<FileUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().mapTo { it.toListFileUi() } },
            onSuccessGetInitData = { files -> _filesUi.update { files } }
        )


    override val isAllFilesUpload: Flow<Boolean> =
        _filesUi.map { it.all { file -> file.uploadState is UploadState.Success } && it.isNotEmpty() }


}

private fun List<FileData>.toListFileUi(): List<FileUi> {
    return this.mapIndexed { ind, file -> file.toFileUI(ind) }
}

private fun FileData.toFileUI(composeKey: Int): FileUi {
    return FileUi(
        id = id,
        composeKey = composeKey,
        platformFile = PlatformFile(path),
        uploadState = UploadState.Success
    )
}





