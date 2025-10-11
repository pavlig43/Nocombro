package ru.pavlig43.declaration.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.DeclarationIn
import ru.pavlig43.core.data.DeclarationOut
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.declaration.api.data.DeclarationUi
import ru.pavlig43.itemlist.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.api.data.DefaultItemFilter
import ru.pavlig43.itemlist.api.data.DefaultItemListRepository
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

abstract class DeclarationTabSlot<Out : DeclarationOut, In : DeclarationIn>(
    componentContext: ComponentContext,
    private val id: Int,
    openDocumentTab: (Int) -> Unit,
    private val documentListRepository: IItemListRepository<Document, DocumentType>,
    private val mapper: DeclarationUi.(id: Int) -> In,
    private val updateRepository: UpdateCollectionRepository<Out, In>,
) : ComponentContext by componentContext, FormTabSlot {
    override val title: String = "Декларации"
    internal val declarationList = DeclarationListComponent<Out>(
        componentContext = childContext("declarationList"),
        getInitData = { updateRepository.getInit(id) },
        openDocumentTab = openDocumentTab
    )
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    internal val dialog: Value<ChildSlot<DialogConfig, MBSItemListComponent<Document, DocumentType>>> = childSlot(
        source = dialogNavigation,
        key = "document_dialog",
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
    ) { config, context ->
        MBSItemListComponent(
            componentContext = context,
            onDismissed = dialogNavigation::dismiss,
            repository = documentListRepository,
            onCreate = { openDocumentTab(0) },
            fullListSelection = listOf(DocumentType.Declaration),
            onItemClick = { id, name ->
                declarationList.addDeclaration(id, name)
                dialogNavigation.dismiss()
            },
            filterFactory = {types,text-> DefaultItemFilter(types,text)},
        )
    }

    private fun showDialog() {
        dialogNavigation.activate(DialogConfig)
    }

    internal fun openDialog() {
        showDialog()
    }


    override suspend fun onUpdate() {
        val old = declarationList.loadInitDataComponent.firstData.value?.map { it.mapper(id) }
        val new = declarationList.declarationUi.value.map { it.mapper(id) }
        updateRepository.update(ChangeSet(old, new))
    }

}


internal class DeclarationListComponent<Out : DeclarationOut>(
    componentContext: ComponentContext,
    val openDocumentTab: (Int) -> Unit,
    private val getInitData: suspend () -> RequestResult<List<Out>>,
) : ComponentContext by componentContext {

    fun removeDeclaration(index: Int) {
        updateList { lst: MutableList<DeclarationUi> ->
            lst.removeIf { it.composeKey == index }
            lst
        }
    }


    fun addDeclaration(documentId: Int, documentName: String) {
        if (documentId in _declarationUiList.value.map { it.documentId }) return
        val composeKey = _declarationUiList.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val declarationUi = DeclarationUi(
            id = 0,
            documentId = documentId,
            isActual = composeKey == 0,
            composeKey = composeKey,
            name = documentName
        )

        updateList { lst ->
            lst.add(declarationUi)
            lst
        }
    }


    private fun updateList(updateAction: (MutableList<DeclarationUi>) -> List<DeclarationUi>) {
        val updatedDeclarations = _declarationUiList.value.toMutableList()
        _declarationUiList.update { updateAction(updatedDeclarations) }
    }


    private val _declarationUiList = MutableStateFlow<List<DeclarationUi>>(emptyList())
    val declarationUi = _declarationUiList.asStateFlow()
    val loadInitDataComponent: ILoadInitDataComponent<List<DeclarationUi>> =
        LoadInitDataComponent<List<DeclarationUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().mapTo { it.toListDeclarationUi() } },
            onSuccessGetInitData = { declarations -> _declarationUiList.update { declarations } }
        )
    fun toggleIsActual(composeKey: Int){
        if (_declarationUiList.value.size < 2) return
        updateList { lst ->
            lst.map { it.copy(isActual = it.composeKey == composeKey) }
        }
    }


}

@Serializable
internal data object DialogConfig

private fun <D : DeclarationOut> List<D>.toListDeclarationUi(): List<DeclarationUi> {
    return this.mapIndexed { ind, declaration -> declaration.toDeclarationUi(ind) }
}


private fun <D : DeclarationOut> D.toDeclarationUi(composeKey: Int): DeclarationUi {
    return DeclarationUi(
        id = id,
        documentId = documentId,
        isActual = isActual,
        composeKey = composeKey,
        name = displayName
    )
}

