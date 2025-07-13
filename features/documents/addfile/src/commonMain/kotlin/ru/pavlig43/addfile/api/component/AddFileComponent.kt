package ru.pavlig43.addfile.api.component


import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.write
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext

class AddFileComponent(
    componentContext: ComponentContext,
) : ComponentContext by componentContext, IAddFileComponent {
    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
//    private val scope: Scope =
//        koinContext.getOrCreateKoinScope()


    override fun addFile(platformFile: PlatformFile) {
        val addedIndex = _addedFiles.value.maxOfOrNull { it.index }?.plus(1) ?: 0
        val startAddedFile = AddedFile(
            index = addedIndex,
            platformFile = platformFile,
            uploadState = UploadState.Loading
        )
        coroutineScope.launch(Dispatchers.IO) {
            loadFile(startAddedFile)
        }

    }

    private suspend fun loadFile(addedFile: AddedFile) {
        val innerFile = PlatformFile(FileKit.filesDir, addedFile.platformFile.name)

        updateList {
            it.add(addedFile.copy(uploadState = UploadState.Loading))
            it
        }

        val result: Result<PlatformFile> = runCatching {
            innerFile.write(addedFile.platformFile)
            innerFile
        }

        val updatedFile = _addedFiles.value.find { it.index == addedFile.index }?.copy(
            platformFile = result.getOrNull() ?: addedFile.platformFile,
            uploadState = if (result.isSuccess) UploadState.Success else UploadState.Error
        )
        updatedFile?.let {file->
            updateList {lst->
                lst[addedFile.index] = file
                lst
            }
        }

    }

    override fun retryLoadFile(index: Int) {
        val file = _addedFiles.value[index]
        updateList { lst->
            lst.removeAt(index)
            lst
        }

        coroutineScope.launch(Dispatchers.IO) {
            loadFile(file)
        }

    }

    override fun openFile(platformFile: PlatformFile) {
        TODO("Not yet implemented")
    }
    private fun updateList(updateAction: ( MutableList<AddedFile>)-> List<AddedFile>){
        val updatedFiles = _addedFiles.value.toMutableList()
        _addedFiles.update { updateAction(updatedFiles) }
    }


    override fun removeFile(index: Int) {
        updateList { lst->
            lst.removeIf{it.index == index}
            lst
        }
    }

    private val _addedFiles = MutableStateFlow<List<AddedFile>>(emptyList())

    override val addedFiles = _addedFiles.asStateFlow()

    override val isAllFilesUpload: Flow<Boolean> = _addedFiles.map { it.all { file -> file.uploadState is UploadState.Success } && it.isNotEmpty() }


}

