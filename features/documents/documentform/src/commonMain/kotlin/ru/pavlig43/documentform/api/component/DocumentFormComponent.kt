package ru.pavlig43.documentform.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.extension
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.scope.Scope
import ru.pavlig43.addfile.api.component.AddFileComponent
import ru.pavlig43.addfile.api.component.IAddFileComponent
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.UTC
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.documentform.api.IDocumentFormDependencies
import ru.pavlig43.documentform.internal.di.createDocumentFormModule
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.api.component.IManageBaseValueItemComponent
import ru.pavlig43.manageitem.api.component.ManageBaseValueItemComponent
import ru.pavlig43.manageitem.api.data.RequireValues
import ru.pavlig43.upsertitem.api.component.ISaveItemComponent
import ru.pavlig43.upsertitem.api.component.SaveItemComponent
import ru.pavlig43.upsertitem.api.data.ItemsForUpsert
import ru.pavlig43.upsertitem.data.ISaveItemRepository
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


    private val saveRepository: ISaveItemRepository<DocumentWithFiles> = scope.get()
    private val initBaseValuesRepository: IInitDataRepository<Document,RequireValues> = scope.get()

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
    private val documentsParamsValidValue: Flow<Boolean> = combine(
        manageBaseValuesOfComponent.isValidAllValue,
        addFileComponent.isAllFilesUpload,
    ){base, isFileUpload-> base && isFileUpload}

    private fun getDocumentsForSave(): ItemsForUpsert<DocumentWithFiles> {
        val requireValues = manageBaseValuesOfComponent.requireValues.value

        val files = addFileComponent.addedFiles.value
        val newDocumentWithFiles = createDocumentWithFiles(requireValues, files)
        val initRequireValues =
            when (val loadState = manageBaseValuesOfComponent.initComponent.loadState.value) {
                is LoadInitDataState.Success<RequireValues> -> loadState.data.copy(type = requireValues.type)
                else -> throw IllegalStateException("Рекомендуемые значения(Имя и тип) при начальной загрузки загрузились с ошибкой ")
            }
        val initFiles =
            when (val loadState = addFileComponent.loadInitDataComponent.loadState.value) {
                is LoadInitDataState.Success<List<AddedFile>> -> loadState.data
                else -> throw IllegalStateException("Список файлов не загрузился")
            }
        val oldDocumentWithFiles = createDocumentWithFiles(initRequireValues, initFiles)
        return ItemsForUpsert(
            newItem = newDocumentWithFiles,
            initItem = oldDocumentWithFiles,

            )
    }
    override val saveDocumentComponent: ISaveItemComponent<DocumentWithFiles> =
        SaveItemComponent(
            componentContext = childContext("saveDocumentComponent"),
            isOtherValidValue = documentsParamsValidValue,
            getItems = ::getDocumentsForSave,
            onSuccessAction = closeTab,
            saveItemRepository = saveRepository
        )


    override fun closeScreen() {
        closeTab()
    }

    @OptIn(ExperimentalTime::class)
    private fun createDocumentWithFiles(
        requireValues: RequireValues,
        filesUi: List<AddedFile>
    ): DocumentWithFiles {
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
        SlotComponent.TabModel("$prefix ${it.type?.displayName ?: ""} ${it.name}")
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

