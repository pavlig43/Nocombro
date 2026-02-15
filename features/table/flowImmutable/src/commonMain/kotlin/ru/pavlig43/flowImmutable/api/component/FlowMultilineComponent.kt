package ru.pavlig43.flowImmutable.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.flowImmutable.api.data.FlowMultilineRepository
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

internal sealed interface ItemListState<out O> {
    data object Loading : ItemListState<Nothing>
    data class Success<O>(val data: List<O>) : ItemListState<O>
    data class Error(val message: String) : ItemListState<Nothing>
}
data class ObservableBDIn<BdIN:CollectionObject>(
    val composeId: Int,
    val bdIn: BdIN,
)

abstract class FlowMultilineComponent<BdOUT: CollectionObject,BdIN:CollectionObject, UI : IMultiLineTableUi, Column>(
    componentContext: ComponentContext,
    parentId: Int,
    getObservableId: (BdIN) -> Int,
    mapper: BdOUT.(Int) -> UI,
    filterMatcher: FilterMatcher<UI, Column>,
    sortMatcher: SortMatcher<UI, Column>,
    private val repository: FlowMultilineRepository<BdIN,BdOUT>,

    ) : ComponentContext by componentContext {


    abstract val columns: ImmutableList<ColumnSpec<UI, Column, TableData<UI>>>


    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<Column>(childContext("filter"))
    private val sortManager = SortManager<Column>(childContext("sort"))
    private val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    private val _observableBDIn = MutableStateFlow<List<ObservableBDIn<BdIN>>>(emptyList())
    private val observableBDIn = _observableBDIn.asStateFlow()


    val initDataComponent = LoadInitDataComponent<List<BdIN>>(
        componentContext = childContext("init"),
        getInitData = {
            repository.getInit(parentId)
        },
        onSuccessGetInitData = { lst ->
            _observableBDIn.update { lst.mapIndexed { index, it -> ObservableBDIn(index, it) } }
        }
    )
    @OptIn(ExperimentalCoroutinesApi::class)
    internal val itemListState = _observableBDIn.flatMapLatest { bdINS ->
        repository.observeOnItemsByIds(bdINS.map{getObservableId(it.bdIn)})
    }.map { result ->
        result.fold(
            onSuccess = { ItemListState.Success(it.mapIndexed { index, oUT -> mapper(oUT,index) })},
            onFailure = { ItemListState.Error(it.message ?: "unknown error") }
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        ItemListState.Loading
    )


    protected fun addParentBD(parentBD: BdIN){
        _observableBDIn.update { lst ->
            val composeId = lst.maxOfOrNull { it.composeId }?.plus(1) ?: 0
            lst + ObservableBDIn(composeId, parentBD)
        }
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

    @Suppress("UNCHECKED_CAST")
    fun onEvent(event: FlowMultiLineEvent) {
        when (event) {

            is FlowMultiLineEvent.DeleteSelected -> {
                _observableBDIn.update { lst ->

                    val updatedList = lst - lst.filter { it.composeId in selectionManager.selectedIds }.toSet()
                    selectionManager.clearSelected()
                    updatedList
                }
            }
            is FlowMultiLineEvent.Selection -> {selectionManager.onEvent(event.selectionUiEvent)}
            is FlowMultiLineEvent.Add<*> -> {addParentBD(event.bdIn as BdIN)}
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
