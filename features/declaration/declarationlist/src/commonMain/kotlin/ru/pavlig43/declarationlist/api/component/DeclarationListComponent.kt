package ru.pavlig43.declarationlist.api.component

import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.pavlig43.core.DeleteState
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.mapTo
import ru.pavlig43.core.toDeleteState
import ru.pavlig43.database.data.declaration.DeclarationIn
import ru.pavlig43.declarationlist.internal.data.DeclarationFilter
import ru.pavlig43.declarationlist.internal.data.DeclarationItemUi
import ru.pavlig43.declarationlist.internal.data.DeclarationListRepository
import ru.pavlig43.declarationlist.internal.data.DeclarationListState


@Suppress("LongParameterList")
class DeclarationListComponent(
    componentContext: ComponentContext,
    tabTitle: String,
    val onCreate: () -> Unit,
    private val repository: DeclarationListRepository,
    val onItemClick: (DeclarationItemUi) -> Unit,
    val withCheckbox: Boolean,
) : ComponentContext by componentContext, SlotComponent {


    private val coroutineScope = componentCoroutineScope()



    private val _model = MutableStateFlow(SlotComponent.TabModel(tabTitle))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()


    private val _searchField = MutableStateFlow("")
    val searchField = _searchField.asStateFlow()
    fun onSearchChange(value: String) {
        _searchField.update { value }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val declarationListState: StateFlow<DeclarationListState> = _searchField.flatMapLatest { searchText->
        repository.observeDeclarationByFilter(DeclarationFilter(searchText,searchText.isNotBlank()))
            .map { result -> result.mapTo { lst -> lst.map {declaration -> declaration.toDeclarationUi() } } }
            .map { it.toItemListState() }
    }
         .stateIn(
                coroutineScope,
                started = Eagerly,
                initialValue = DeclarationListState.Loading()
            )

    val deleteState: MutableStateFlow<DeleteState> =
        MutableStateFlow(DeleteState.Initial())


    private val _selectedItemIds = mutableStateListOf<Int>()
    val selectedItemIds: List<Int>
        get() = _selectedItemIds

    fun actionInSelectedItemIds(checked: Boolean, id: Int) {
        if (checked) {
            _selectedItemIds.add(id)
        } else {
            _selectedItemIds.remove(id)
        }
    }


    fun deleteItems(ids: List<Int>) {

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

    fun shareItems(ids: List<Int>) {
        println(ids)
        TODO("Not yet implemented")
    }
}

private fun RequestResult<List<DeclarationItemUi>>.toItemListState(): DeclarationListState {
    return when (this) {
        is RequestResult.Error -> DeclarationListState.Error(message ?: "unknown error")
        is RequestResult.InProgress -> DeclarationListState.Loading()
        is RequestResult.Initial -> DeclarationListState.Initial()
        is RequestResult.Success<List<DeclarationItemUi>> -> DeclarationListState.Success(data)
    }
}

private fun DeclarationIn.toDeclarationUi(): DeclarationItemUi {
    return DeclarationItemUi(
        id = id,
        displayName = displayName,
        createdAt = createdAt,
        vendorId = vendorId,
        vendorName = vendorName,
        bestBefore = bestBefore,
    )
}


