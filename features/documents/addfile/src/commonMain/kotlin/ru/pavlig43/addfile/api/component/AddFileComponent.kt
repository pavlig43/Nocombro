package ru.pavlig43.addfile.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
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
import org.koin.core.scope.Scope
import ru.pavlig43.addfile.api.IAddFileDependencies
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.addfile.internal.di.createAddFileModuleFactory
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository

class AddFileComponent(
    componentContext: ComponentContext,
    dependencies: IAddFileDependencies,
    documentId: Int,
) : ComponentContext by componentContext, IAddFileComponent {
    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createAddFileModuleFactory(dependencies))

    private val initDataRepository:IInitDataRepository<List<AddedFile>> = scope.get()


    override fun addFile(platformFile: PlatformFile) {
        val addedIndex = _addedFiles.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val startAddedFile = AddedFile(
            id = 0,
            composeKey = addedIndex,
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

        val updatedFile = _addedFiles.value.find { it.composeKey == addedFile.composeKey }?.copy(
            platformFile = result.getOrNull() ?: addedFile.platformFile,
            uploadState = if (result.isSuccess) UploadState.Success else UploadState.Error
        )
        updatedFile?.let { file ->
            updateList { lst ->
                lst[addedFile.composeKey] = file
                lst
            }
        }

    }

    override fun retryLoadFile(index: Int) {
        val file = _addedFiles.value[index]
        updateList { lst ->
            lst.removeAt(index)
            lst
        }

        coroutineScope.launch(Dispatchers.IO) {
            loadFile(file)
        }

    }


    private fun updateList(updateAction: (MutableList<AddedFile>) -> List<AddedFile>) {
        val updatedFiles = _addedFiles.value.toMutableList()
        _addedFiles.update { updateAction(updatedFiles) }
    }


    override fun removeFile(index: Int) {
        updateList { lst ->
            lst.removeIf { it.composeKey == index }
            lst
        }
    }

    private val _addedFiles = MutableStateFlow<List<AddedFile>>(initDataRepository.getInitDataForState())
    override val loadInitDataComponent: ILoadInitDataComponent<List<AddedFile>> = LoadInitDataComponent<List<AddedFile>>(
        componentContext = childContext("loadInitData"),
        id = documentId,
        initDataRepository = initDataRepository,
        onSuccessGetInitData = {files->_addedFiles.update { files }}
    )


    override val addedFiles = _addedFiles.asStateFlow()

    override val isAllFilesUpload: Flow<Boolean> =
        _addedFiles.map { it.all { file -> file.uploadState is UploadState.Success } && it.isNotEmpty() }



}





