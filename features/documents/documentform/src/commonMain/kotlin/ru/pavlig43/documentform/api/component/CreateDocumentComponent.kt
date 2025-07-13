package ru.pavlig43.documentform.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import ru.pavlig43.addfile.api.component.AddFileComponent
import ru.pavlig43.addfile.api.component.IAddFileComponent
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.UTC
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.createitem.api.component.CreateItemComponent
import ru.pavlig43.createitem.api.component.ICreateItemComponent
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.documentform.internal.data.CreateDocumentRepository
import ru.pavlig43.documentform.internal.data.toDocumentFilePath
import ru.pavlig43.documentform.internal.di.createModule
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CreateDocumentComponent(
    private val closeTab:()->Unit,
    componentContext: ComponentContext,
    dependencies: IDocumentFormDependencies,
) : ComponentContext by componentContext, IDocumentFormComponent, SlotComponent {

    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope = koinContext.getOrCreateKoinScope(createModule(dependencies))

    private val repository: CreateDocumentRepository = scope.get()

    override val createBaseRowsOfComponent: ICreateItemComponent =
        CreateItemComponent<Document, DocumentType>(
            componentContext = childContext("createBaseRowsOfComponent"),
            repository = scope.get(),
            typeVariantList = DocumentType.entries
        )
    override val addFileComponent: IAddFileComponent =
        AddFileComponent(
            componentContext = childContext("addFileComponent")
        )
    private val _saveDocumentState: MutableStateFlow<SaveDocumentState> = MutableStateFlow(SaveDocumentState.Init())
    override val saveDocumentState: StateFlow<SaveDocumentState> = _saveDocumentState.asStateFlow()

    override val isValidAllValue: StateFlow<Boolean> =
        combine(
            createBaseRowsOfComponent.isValidAllValue,
            addFileComponent.isAllFilesUpload,
            _saveDocumentState
        ) { base, isFileUpload, saveState ->
            base && isFileUpload && saveState is SaveDocumentState.Init
        }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            false
        )

    @OptIn(ExperimentalTime::class)
    override fun saveDocument() {
        _saveDocumentState.update { SaveDocumentState.Loading() }
        coroutineScope.launch {
            delay(2000)
            val documentName = createBaseRowsOfComponent.name.value
            val documentType =
                createBaseRowsOfComponent.type.value?.let { it as DocumentType } ?: return@launch
            val createdAt = Clock.System.now().toEpochMilliseconds()
            val document = Document(documentName, documentType, UTC(createdAt))
            val files = addFileComponent.addedFiles.value
            val result = repository.saveItem(
                DocumentWithFiles(
                    document = document,
                    files = files.map { it.toDocumentFilePath() }
                )
            )
            _saveDocumentState.update { result.toSaveStateDocument() }
        }
    }

    override fun closeScreen() {
        closeTab()
    }


    private val _model = MutableStateFlow(SlotComponent.TabModel(TAB_TITLE))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    private companion object {
        const val TAB_TITLE = "* Создание документа"
    }

}
private fun RequestResult<Unit>.toSaveStateDocument(): SaveDocumentState {
    return when(this){
        is RequestResult.Error<*> -> SaveDocumentState.Error(this.message?:"Неизвестная ошибка")
        is RequestResult.InProgress -> SaveDocumentState.Loading()
        is RequestResult.Initial<*> -> SaveDocumentState.Init()
        is RequestResult.Success<*> -> SaveDocumentState.Success()
    }
}

