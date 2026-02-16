package ru.pavlig43.immutable.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.immutable.api.component.ImmutableTableBuilderData
import ru.pavlig43.immutable.internal.component.manager.DeleteManager
import ru.pavlig43.immutable.internal.data.ImmutableListRepository
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


@Suppress("LongParameterList")
internal abstract class ImmutableTableComponent<BD, UI : IMultiLineTableUi, Column>(
    componentContext: ComponentContext,
    tableBuilder: ImmutableTableBuilderData<UI>,
    val onCreate: () -> Unit,
    val onItemClick: (UI) -> Unit,
    mapper: BD.() -> UI,
    filterMatcher: FilterMatcher<UI, Column>,
    sortMatcher: SortMatcher<UI, Column>,
    val repository: ImmutableListRepository<BD>,

    ) : ComponentContext by componentContext {


    abstract val columns: ImmutableList<ColumnSpec<UI, Column, TableData<UI>>>


    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<Column>(childContext("filter"))
    private val sortManager = SortManager<Column>(childContext("sort"))
    private val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    private val deleteManager = DeleteManager(
        componentContext = childContext("delete"),
        clearSelection = selectionManager::clearSelected,
        deleteFn = repository::deleteByIds
    )


    val itemListState = repository.observeOnItems(tableBuilder.parentId).map { result ->
        result.fold(
            onSuccess = { ItemListState.Success(it.map(mapper)) },
            onFailure = { ItemListState.Error(it.message ?: "unknown error") }
        )
    }
        .stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            ItemListState.Loading
        )


    val tableData = combine(
        itemListState,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { state, selectedIds, filters, sort ->
        when (state) {
            is ItemListState.Error,
            is ItemListState.Loading -> TableData(isSelectionMode = tableBuilder.withCheckbox)

            is ItemListState.Success -> {
                val filtered = state.data.filter { ui ->
                    filterMatcher.matchesItem(ui, filters)
                }
                val displayed = sortMatcher.sort(filtered, sort)
                TableData(
                    displayedItems = displayed,
                    selectedIds = selectedIds,
                    isSelectionMode = tableBuilder.withCheckbox
                )

            }
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = tableBuilder.withCheckbox)
    )

    fun onEvent(event: ImmutableTableUiEvent) {
        when (event) {
            is ImmutableTableUiEvent.Selection -> {
                selectionManager.onEvent(event.selectionUiEvent)
            }

            ImmutableTableUiEvent.DeleteSelected -> {
                deleteManager.deleteSelected(selectionManager.selectedIds)
            }

            ImmutableTableUiEvent.CreateNewItem -> {
                onCreate()
            }
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


