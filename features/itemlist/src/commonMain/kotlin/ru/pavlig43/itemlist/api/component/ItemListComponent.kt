package ru.pavlig43.itemlist.api.component


import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.itemlist.api.data.ItemListRepository
import ru.pavlig43.itemlist.api.data.ItemUi


class ItemListComponent<I : Item, S : ItemType>(
    componentContext: ComponentContext,
    override val fullListSelection: List<S>,
    tabTitle: String,
    override val onCreate: () -> Unit,
    private val repository: ItemListRepository<I, S>,
    override val onItemClick: (id: Int, String) -> Unit,
    override val withCheckbox: Boolean,
) : ComponentContext by componentContext, IItemListComponent, SlotComponent {
    private val coroutineScope = componentCoroutineScope()

    private val _model = MutableStateFlow(SlotComponent.TabModel(tabTitle))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()


    private val selectedItemTypes = MutableStateFlow<List<S>>(fullListSelection)

    private val _searchField = MutableStateFlow("")
    override val searchField = _searchField.asStateFlow()
    override fun onSearchChange(value: String) {
        _searchField.update { value }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override val itemListState: StateFlow<ItemListState> =
        combine(
            selectedItemTypes,
            _searchField
        ) { types, searchText ->
            repository.observeItemsByFilter(types, searchText).map { it.toItemListState() }
        }.flatMapLatest {it }
            .stateIn(
                coroutineScope,
                started = Eagerly,
                initialValue = ItemListState.Loading()
            )

    override val deleteState: MutableStateFlow<DeleteState> =
        MutableStateFlow(DeleteState.Initial())


    @Suppress("UNCHECKED_CAST")
    override fun saveSelection(selection: List<ItemType>) {
        selectedItemTypes.update { selection as List<S> }

    }

    private val _selectedItemIds = mutableStateListOf<Int>()
    override val selectedItemIds: List<Int>
        get() = _selectedItemIds

    override fun actionInSelectedItemIds(checked: Boolean, id: Int) {
        if (checked) {
            _selectedItemIds.add(id)
        } else {
            _selectedItemIds.remove(id)
        }
    }


    override fun deleteItems(ids: List<Int>) {

        coroutineScope.launch {
            deleteState.update { DeleteState.Loading() }
            val state = repository.deleteItemsById(ids).toDeleteState()
            deleteState.update { state }
            if (state is DeleteState.Success) {
                _selectedItemIds.clear()
            }
            deleteState.update { DeleteState.Initial() }
        }

    }

    override fun shareItems(ids: List<Int>) {
        TODO("Not yet implemented")
    }
}

private fun RequestResult<List<ItemUi>>.toItemListState(): ItemListState {
    return when (this) {
        is RequestResult.Error -> ItemListState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState.Loading()
        is RequestResult.Initial -> ItemListState.Initial()
        is RequestResult.Success<List<ItemUi>> -> ItemListState.Success(data)
    }
}

private fun RequestResult<Unit>.toDeleteState(): DeleteState {
    return when (this) {
        is RequestResult.Error<*> -> DeleteState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> DeleteState.Loading()
        is RequestResult.Initial<*> -> DeleteState.Initial()
        is RequestResult.Success<*> -> DeleteState.Success("success")
    }
}




