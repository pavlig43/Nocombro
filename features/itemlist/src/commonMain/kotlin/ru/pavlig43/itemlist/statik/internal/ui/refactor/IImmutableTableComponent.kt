package ru.pavlig43.itemlist.statik.internal.ui.refactor

import androidx.compose.runtime.Immutable
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentListRepository


@Immutable
data class DocumentTableData(

    override val displayedItems:List<DocumentItemUi> = emptyList(),
    /** IDs of selected  */
    override val selectedIds: Set<Int> = emptySet(),
    /** Whether selection mode is enabled */
    override val selectionModeEnabled: Boolean = false,
): TableData<DocumentItemUi>
interface TableData<I: Any>{
    val displayedItems:List<I>
    val selectedIds: Set<Int>
    val selectionModeEnabled: Boolean
}
interface IImmutableTableComponent<I : Any> {
    val itemListState: StateFlow<ItemListState1<I>>

    val tableData: StateFlow<TableData<I>>
    fun onEvent(event: SampleUiEvent)
}

sealed interface ItemListState1<out O : Any> {
    class Loading : ItemListState1<Nothing>
    class Success<O : Any>(val data: List<O>) : ItemListState1<O>
    class Error<O : Any>(val message: String) : ItemListState1<O>
}

fun <BD : GenericItem, UI : Any> RequestResult<List<BD>>.toItemListState(mapper: BD.() -> UI): ItemListState1<UI> {
    return when (this) {
        is RequestResult.Error -> ItemListState1.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState1.Loading()
        is RequestResult.Initial -> ItemListState1.Loading()
        is RequestResult.Success<List<BD>> -> ItemListState1.Success(data.map(mapper))
    }
}

internal sealed interface DeleteState {
    class Initial : DeleteState
    class Loading : DeleteState
    class Success() : DeleteState
    class Error(val message: String) : DeleteState
}

private fun RequestResult<Unit>.toDeleteState(): DeleteState {
    return when (this) {
        is RequestResult.Error<*> -> DeleteState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> DeleteState.Loading()
        is RequestResult.Initial<*> -> DeleteState.Initial()
        is RequestResult.Success<*> -> DeleteState.Success()
    }
}

internal class DocumentTableComponent(
    componentContext: ComponentContext,
    val onCreate: () -> Unit,
    val onItemClick: (DocumentItemUi) -> Unit,
    private val repository: DocumentListRepository
) : ComponentContext by componentContext, IImmutableTableComponent<DocumentItemUi> {

    private val coroutineScope = componentCoroutineScope()
    private val _deleteState = MutableStateFlow<DeleteState>(DeleteState.Initial())
    val deleteState = _deleteState.asStateFlow()


    private val _tableData = MutableStateFlow(DocumentTableData())
    override val tableData = _tableData.asStateFlow()


    override val itemListState: StateFlow<ItemListState1<DocumentItemUi>> =
        repository.observeOnItems().map {
            it.toItemListState(Document::toUi)
        }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            ItemListState1.Loading()
        )


    override fun onEvent(event: SampleUiEvent) {
        when (event) {
            is SampleUiEvent.ClearSelection -> clearSelected()
            is SampleUiEvent.DeleteSelected -> deleteSelected()
            is SampleUiEvent.ToggleSelectAll -> toggleSelectAll()
            is SampleUiEvent.ToggleSelection -> toggleSelection(event.documentId)
        }
    }

    private fun toggleSelection(documentId: Int) {
        updateTableData {

            val newSelectedIds = if (documentId in selectedIds) {
                selectedIds - documentId
            } else {
                selectedIds + documentId
            }

            copy(selectedIds = newSelectedIds)
        }
    }

    private fun toggleSelectAll() {
        updateTableData {
            val displayedIds = displayedItems.map { it.id }.toSet()
            val selectedIds = if (displayedIds.all { it in selectedIds }) {
                selectedIds - displayedIds
            } else {
                selectedIds + displayedIds
            }
            copy(selectedIds = selectedIds)
        }
    }

    private fun clearSelected() {
        updateTableData {
            copy(selectedIds = emptySet())
        }
    }

    private fun deleteSelected() {
        coroutineScope.launch {
            val idsForDelete = _tableData.value.selectedIds
            _deleteState.update { DeleteState.Loading() }
            val deleteResult = repository.deleteByIds(idsForDelete)
            _deleteState.update { deleteResult.toDeleteState() }
            if (deleteResult is RequestResult.Success) {
                clearSelected()
            }

        }

    }


    private fun updateTableData(action: DocumentTableData.() -> DocumentTableData) {
        _tableData.update { tableData ->
            tableData.action()
        }
    }

}

private fun Document.toUi(): DocumentItemUi {
    return DocumentItemUi(
        id = id,
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment
    )
}
