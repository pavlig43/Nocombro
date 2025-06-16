package ru.pavlig43.itemlist.api.component


import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.common.data.Item
import ru.pavlig43.database.data.common.data.ItemType
import ru.pavlig43.itemlist.api.data.IItemRepository
import ru.pavlig43.itemlist.api.data.ItemUi
import ru.pavlig43.itemlist.internal.di.createItemListModule

class ItemListComponent<I : Item, U : ItemUi, S : ItemType>(
    componentContext: ComponentContext,
    private val onCreateScreen: () -> Unit,
    private val repository: IItemRepository<I, U, S>,
) : ComponentContext by componentContext, IItemListComponent {
    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createItemListModule())

    private val selectedItemTypes = MutableStateFlow<List<S>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    override val itemListState: StateFlow<ItemListState> =
        selectedItemTypes.flatMapLatest { types ->
            if (types.isEmpty()) {
                repository.getAllItem()
            } else {
                repository.getItemsByTypes(types)

            }.map { requestResult -> requestResult.toItemListState(repository::toItemUi) }
        }.stateIn(
            coroutineScope,
            started = Eagerly,
            initialValue = ItemListState.Loading()
        )
    override val deleteState:MutableStateFlow<DeleteState> = MutableStateFlow(DeleteState.Initial())


    override fun onCreate() {
       onCreateScreen()
    }

    @Suppress("UNCHECKED_CAST")
    override fun saveSelection(selection: List<ItemType>) {
        selectedItemTypes.update { selection as List<S> }

    }
    private val _selectedItemIds = mutableStateListOf<Int>()
    override val selectedItemIds: List<Int>
        get() = _selectedItemIds

    override fun actionInSelectedItemIds(checked:Boolean,id:Int) {
        if (checked) {
            _selectedItemIds.add(id)
        } else {
            _selectedItemIds.remove(id)
        }
    }

    override val fullListSelection: List<ItemType> = repository.getAllItemTypes()
    override fun deleteItems(ids: List<Int>) {

        coroutineScope.launch {
            deleteState.update { DeleteState.Loading() }
            val state = repository.deleteItemsById(ids).toDeleteState()
            deleteState.update { state }
            delay(1000)
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


private fun <I : Item, O : ItemUi> RequestResult<List<I>>.toItemListState(
    mapper: (I) -> O,
): ItemListState {
    return when (this) {
        is RequestResult.Error -> ItemListState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState.Loading()
        is RequestResult.Initial -> ItemListState.Initial()
        is RequestResult.Success<List<I>> -> ItemListState.Success(data.map(mapper))
    }
}
private fun RequestResult<Int>.toDeleteState(): DeleteState {
    return when (this) {
        is RequestResult.Error<*> -> DeleteState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> DeleteState.Loading()
        is RequestResult.Initial<*> -> DeleteState.Initial()
        is RequestResult.Success<*> -> DeleteState.Success("success")
    }
}



