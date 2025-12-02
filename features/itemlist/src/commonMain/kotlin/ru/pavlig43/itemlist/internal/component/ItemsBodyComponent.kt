package ru.pavlig43.itemlist.internal.component

import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter

@Suppress("LongParameterList")
internal class ItemsBodyComponent<O : GenericItem, U : IItemUi>(
    componentContext: ComponentContext,
    dataFlow: Flow<RequestResult<List<O>>>,
    val searchText: StateFlow<ItemFilter.SearchText>,
    private val deleteItemsById: suspend (List<Int>) -> RequestResult<Unit>,
    val withCheckbox: Boolean,
    val onItemClick: (U) -> Unit,
    private val mapper: O.() -> U,

    ) : ComponentContext by componentContext {
    private val coroutineScope = componentCoroutineScope()


    private val _selectedItemIds = mutableStateListOf<Int>()
    val selectedItemIds: List<Int>
        get() = _selectedItemIds

    fun clearSelectedIds() {
        _selectedItemIds.clear()
    }

    fun actionInSelectedItemIds(checked: Boolean, id: Int) {
        if (checked) {
            _selectedItemIds.add(id)
        } else {
            _selectedItemIds.remove(id)
        }
    }



    val itemListState: StateFlow<ItemListState<U>> = dataFlow
    .map { it.toItemListState(mapper)  }
        .stateIn(
            coroutineScope,
            started = SharingStarted.Companion.Eagerly,
            initialValue = ItemListState.Loading()
        )


    val deleteState: MutableStateFlow<DeleteState> =
        MutableStateFlow(DeleteState.Initial())

    fun deleteItems() {
        coroutineScope.launch {
            deleteState.update { DeleteState.Loading() }
            val state = deleteItemsById(_selectedItemIds).toDeleteState()
            deleteState.update { state }
            if (state is DeleteState.Success) {
                clearSelectedIds()
            }
            deleteState.update { DeleteState.Initial() }
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

