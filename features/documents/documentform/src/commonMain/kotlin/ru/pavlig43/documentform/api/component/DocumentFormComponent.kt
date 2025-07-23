package ru.pavlig43.documentform.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.extension
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import ru.pavlig43.addfile.api.component.AddFileComponent
import ru.pavlig43.addfile.api.component.IAddFileComponent
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.UTC
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.manageitem.api.component.ManageBaseValueItemComponent
import ru.pavlig43.manageitem.api.component.IManageBaseValueItemComponent
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.documentform.internal.data.ISaveDocumentRepository
import ru.pavlig43.documentform.internal.data.toSaveStateDocument
import ru.pavlig43.documentform.internal.di.createDocumentFormModule
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.api.data.RequireValues
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DocumentFormComponent(
    private val documentId: Int,
    private val closeTab: () -> Unit,
    componentContext: ComponentContext,
    dependencies: IDocumentFormDependencies,
) : ComponentContext by componentContext, IDocumentFormComponent, SlotComponent {

    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createDocumentFormModule(dependencies))

    private val saveRepository: ISaveDocumentRepository = scope.get()
    private val initBaseValuesRepository: IInitDataRepository<RequireValues> = scope.get()

    override val manageBaseValuesOfComponent: IManageBaseValueItemComponent =
        ManageBaseValueItemComponent(
            componentContext = childContext("manageBaseValuesOfComponent"),
            typeVariantList = DocumentType.entries,
            id = documentId,
            initDataRepository = initBaseValuesRepository

        )
    override val addFileComponent: IAddFileComponent =
        AddFileComponent(
            componentContext = childContext("addFileComponent"),
            dependencies = scope.get(),
            documentId = documentId
        )
    private val _saveDocumentState: MutableStateFlow<SaveDocumentState> =
        MutableStateFlow(SaveDocumentState.Init())
    override val saveDocumentState: StateFlow<SaveDocumentState> = _saveDocumentState.asStateFlow()

    override val isValidAllValue: StateFlow<Boolean> =
        combine(
            manageBaseValuesOfComponent.isValidAllValue,
            addFileComponent.isAllFilesUpload,
            _saveDocumentState
        ) { base, isFileUpload, saveState ->
            base && isFileUpload && (saveState is SaveDocumentState.Init || saveState is SaveDocumentState.Error)
        }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            false
        )

    override fun saveDocument() {
        _saveDocumentState.update { SaveDocumentState.Loading() }
        coroutineScope.launch {
            val requireValues = manageBaseValuesOfComponent.requireValues.value

            val files = addFileComponent.addedFiles.value
            val newDocumentWithFiles = createDocumentWithFiles(requireValues, files)
            val initRequireValues =
                when (val loadState = manageBaseValuesOfComponent.initComponent.loadState.value) {
                    is LoadInitDataState.Success<RequireValues> -> loadState.data.copy(type = requireValues.type)
                    else -> return@launch
                }
            val initFiles =
                when (val loadState = addFileComponent.loadInitDataComponent.loadState.value) {
                    is LoadInitDataState.Success<List<AddedFile>> -> loadState.data
                    else -> return@launch
                }
            val oldDocumentWithFiles = createDocumentWithFiles(initRequireValues, initFiles)

            val result = saveRepository.saveItem(
                documentForSave = newDocumentWithFiles,
                initLoadDocument = oldDocumentWithFiles
            )
            _saveDocumentState.update { result.toSaveStateDocument() }
        }
    }

    override fun closeScreen() {
        closeTab()
    }

    @OptIn(ExperimentalTime::class)
    private fun createDocumentWithFiles(
        requireValues: RequireValues,
        filesUi: List<AddedFile>
    ): DocumentWithFiles {
        println("requireValues $requireValues")
        val document = Document(
            id = requireValues.id,
            displayName = requireValues.name,
            type = requireValues.type as DocumentType,
            createdAt = requireValues.createdAt ?: UTC(Clock.System.now().toEpochMilliseconds()),
        )
        val files = filesUi.map { it.toDocumentFilePath(requireValues.id) }
        return DocumentWithFiles(
            document = document,
            files = files
        )
    }


    override val model = manageBaseValuesOfComponent.requireValues.map {
        val prefix = if (documentId == 0) "* " else ""
        SlotComponent.TabModel("$prefix ${it.type ?: ""} ${it.name}")
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        SlotComponent.TabModel("")
    )

}

private fun AddedFile.toDocumentFilePath(documentId: Int): DocumentFilePath {
    return DocumentFilePath(
        documentId = documentId,
        filePath = platformFile.absolutePath(),
        fileExtension = platformFile.extension,
        id = this.id
    )
}

