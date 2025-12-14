package ru.pavlig43.itemlist.statik.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.core.component.ValueFilterComponent
import ru.pavlig43.itemlist.statik.internal.component.core.SelectedRowsComponent

@Suppress("LongParameterList")
internal class StaticListComponent<BDOut : GenericItem, UI : IItemUi>(
    componentContext: ComponentContext,
    dataFlow: Flow<RequestResult<List<BDOut>>>,
    val onCreate: () -> Unit,
    val searchTextComponent: ValueFilterComponent<String>,
    private val deleteItemsById: suspend (List<Int>) -> RequestResult<Unit>,
    val withCheckbox: Boolean,
    val onItemClick: (UI) -> Unit,
    private val mapper: BDOut.() -> UI,

    ) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()

    val selectedRowsComponent = SelectedRowsComponent(childContext("selectedRows"))


    val itemListState: StateFlow<ItemListState<UI>> = dataFlow
    .map { it.toItemListState(mapper)  }
        .stateIn(
            coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = ItemListState.Loading()
        )


    val deleteState: MutableStateFlow<DeleteState> =
        MutableStateFlow(DeleteState.Initial())

    fun deleteItems() {
        coroutineScope.launch {
            deleteState.update { DeleteState.Loading() }
            val state = deleteItemsById(selectedRowsComponent.selectedItemIds).toDeleteState()
            deleteState.update { state }
            if (state is DeleteState.Success) {
                selectedRowsComponent.clearSelectedIds()
                deleteState.update { DeleteState.Initial() }
            }
            if (state is DeleteState.Error){
                deleteState.update { DeleteState.Error(message = state.message) }
            }

        }

    }

    fun shareItems() {
        TODO("Not yet implemented")
    }

}

internal sealed interface DeleteState{
    class Initial : DeleteState
    class Loading : DeleteState
    class Success(val message: String) : DeleteState
    class Error(val message: String) : DeleteState
}
internal fun RequestResult<Unit>.toDeleteState(): DeleteState {
    return when (this) {
        is RequestResult.Error<*> -> DeleteState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> DeleteState.Loading()
        is RequestResult.Initial<*> -> DeleteState.Initial()
        is RequestResult.Success<*> -> DeleteState.Success("success")
    }
}
internal sealed interface ItemListState<out O : IItemUi> {
    class Initial : ItemListState<Nothing>
    class Loading : ItemListState<Nothing>
    class Success<O : IItemUi>(val data: List<O>) : ItemListState<O>
    class Error<O : IItemUi>(val message: String) : ItemListState<O>
}
private fun <O : GenericItem,U: IItemUi> RequestResult<List<O>>.toItemListState(mapper:O.()->U): ItemListState<U> {
    return when (this) {
        is RequestResult.Error -> ItemListState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState.Loading()
        is RequestResult.Initial -> ItemListState.Initial()
        is RequestResult.Success<List<O>> -> ItemListState.Success(data.map { it.mapper() })
    }
}

