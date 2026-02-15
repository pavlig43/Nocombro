package ru.pavlig43.mutable.api.flowMiltiline.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.mutable.api.flowMiltiline.data.FlowMultilineRepository
import ru.pavlig43.tablecore.manger.FilterManager
import ru.pavlig43.tablecore.manger.SelectionManager
import ru.pavlig43.tablecore.manger.SortManager
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.utils.FilterMatcher
import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState
import kotlin.map

internal sealed interface ItemListState<out O> {
    data object Loading : ItemListState<Nothing>
    data class Success<O>(val data: List<O>) : ItemListState<O>
    data class Error(val message: String) : ItemListState<Nothing>
}


abstract class FlowMultilineComponent<BD, UI : IMultiLineTableUi, Column>(
    componentContext: ComponentContext,
    parentId: Int,
    mapper: BD.() -> UI,
    filterMatcher: FilterMatcher<UI, Column>,
    sortMatcher: SortMatcher<UI, Column>,
    val repository: FlowMultilineRepository<BD>,

    ) : ComponentContext by componentContext {


    abstract val columns: ImmutableList<ColumnSpec<UI, Column, TableData<UI>>>


    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<Column>(childContext("filter"))
    private val sortManager = SortManager<Column>(childContext("sort"))
    private val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    private val observableIds = MutableStateFlow(emptyList<Int>())
    val initDataComponent = LoadInitDataComponent<List<Int>>(
        componentContext = childContext("init"),
        getInitData = {
            repository.getInit(parentId)
        },
        onSuccessGetInitData = { lst ->
            observableIds.update { lst }
        }
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val itemListState = observableIds.flatMapLatest { ids ->
        repository.observeOnItemsByIds(ids)
    }.map { result ->
        result.fold(
            onSuccess = { ItemListState.Success(it.map(mapper)) },
            onFailure = { ItemListState.Error(it.message ?: "unknown error") }
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        ItemListState.Loading
    )


    protected fun addId(id: Int){
        observableIds.update { it + id }
    }

    internal val tableData = combine(
        itemListState,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { state, selectedIds, filters, sort ->
        when (state) {
            is ItemListState.Error,
            is ItemListState.Loading -> TableData(isSelectionMode = true)

            is ItemListState.Success -> {
                val filtered = state.data.filter { ui ->
                    filterMatcher.matchesItem(ui, filters)
                }
                val displayed = sortMatcher.sort(filtered, sort)
                TableData(
                    displayedItems = displayed,
                    selectedIds = selectedIds,
                    isSelectionMode = true
                )

            }
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = true)
    )

    fun onEvent(event: FlowMultiLineEvent) {
        when (event) {

            is FlowMultiLineEvent.CallChoiceDialog -> {addId(event.id)}

            is FlowMultiLineEvent.DeleteSelected -> {
                observableIds.update { lst ->

                    val updatedList = lst - selectionManager.selectedIds
                    selectionManager.clearSelected()
                    updatedList
                }
            }
            is FlowMultiLineEvent.Selection -> {selectionManager.onEvent(event.selectionUiEvent)}
        }
    }



    fun updateFilters(filters: Map<Column, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<Column>?) {
        sortManager.update(sort)
    }
}
