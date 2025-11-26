package ru.pavlig43.itemlist.api.component.refactoring

import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.DeleteState
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.itemlist.api.component.ItemListState1
import ru.pavlig43.itemlist.api.component.toDeleteState


class ItemsBodyComponent<O : GenericItem, U : IItemUi>(
    componentContext: ComponentContext,
    dataFlow: Flow<RequestResult<List<O>>>,
    val searchText: StateFlow<ItemFilter1.SearchText>,
    private val deleteItemsById: suspend (List<Int>) -> RequestResult<Unit>,
    val withCheckbox: Boolean,
    val onItemClick: (U) -> Unit,
    private val mapper: O.() -> U,
    val onCreate: () -> Unit

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



    val itemListState: StateFlow<ItemListState1<U>> = dataFlow
    .map { it.toItemListState1(mapper)  }
        .stateIn(
            coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = ItemListState1.Loading()
        )


    val deleteState: MutableStateFlow<DeleteState> =
        MutableStateFlow(DeleteState.Initial())

    fun deleteItems(ids: List<Int>) {
        coroutineScope.launch {
            deleteState.update { DeleteState.Loading() }
            val state = deleteItemsById(ids).toDeleteState()
            deleteState.update { state }
            if (state is DeleteState.Success) {
                clearSelectedIds()
            }
            deleteState.update { DeleteState.Initial() }
        }

    }

    fun shareItems(ids: List<Int>) {
        TODO("Not yet implemented")
    }

}


internal fun <O : GenericItem,U: IItemUi> RequestResult<List<O>>.toItemListState1(mapper:O.()->U): ItemListState1<U> {
    return when (this) {
        is RequestResult.Error -> ItemListState1.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState1.Loading()
        is RequestResult.Initial -> ItemListState1.Initial()
        is RequestResult.Success<List<O>> -> ItemListState1.Success(data.map { it.mapper() })
    }
}



