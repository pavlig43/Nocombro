package ru.pavlig43.itemlist.refactor

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.SlotComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.itemlist.core.data.IItemUi
import ru.pavlig43.itemlist.refactor.manager.DeleteManager
import ru.pavlig43.itemlist.refactor.manager.FilterManager
import ru.pavlig43.itemlist.refactor.manager.SelectionManager
import ru.pavlig43.itemlist.refactor.manager.SortManager
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.statik.internal.component.ImmutableListRepository
import ru.pavlig43.itemlist.statik.internal.di.moduleFactory
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState
import kotlin.collections.map

interface FilterMatcher<I : IItemUi, C> {
    fun matchesItem(item: I, filters: Map<C, TableFilterState<*>>): Boolean
}

interface SortMatcher<I : IItemUi, C> {
    fun sort(items: List<I>, sort: SortState<C>?): List<I>
}

abstract class ImmutableTableComponent<BD, UI : IItemUi, C>(
    componentContext: ComponentContext,
    private val withCheckbox: Boolean,
    val onCreate: () -> Unit,
    val onItemClick: (UI) -> Unit,
//    repository: ImmutableListRepository<BD>,
    mapper: BD.() -> UI,
    filterMatcher: FilterMatcher<UI, C>,
    sortMatcher: SortMatcher<UI, C>,
    dependencies: ItemStaticListDependencies,
) : ComponentContext by componentContext{

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(
        moduleFactory(dependencies)
    )

    private val repository: ImmutableListRepository<BD> = scope.get()
    val selectionManager = SelectionManager(childContext("selection"))
    val filterManager = FilterManager<C>(childContext("filter"))
    val sortManager = SortManager<C>(childContext("sort"))

    internal val deleteManager = DeleteManager(
        componentContext = childContext("delete"),
        clearSelection = selectionManager::clearSelected,
        deleteFn = repository::deleteByIds
    )

    private val coroutineScope = componentCoroutineScope()


    val itemListState =
        repository.observeOnItems().map {
            it.toItemListState(mapper)
        }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            ItemListState1.Loading()
        )


    val tableData = combine(
        itemListState,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { state, selectedIds, filters, sort ->
        when (state) {
            is ItemListState1.Error,
            is ItemListState1.Loading -> TableData1(isSelectionMode = withCheckbox)

            is ItemListState1.Success -> {
                val filtered = state.data.filter { ui ->
                    filterMatcher.matchesItem(ui, filters)
                }
                val displayed = sortMatcher.sort(filtered, sort)
                TableData1(
                    displayedItems = displayed,
                    selectedIds = selectedIds,
                    isSelectionMode = withCheckbox
                )

            }
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData1(isSelectionMode = withCheckbox)
    )


    fun onEvent(event: SelectionUiEvent) {
        when (event) {
            is SelectionUiEvent.ClearSelection -> selectionManager::clearSelected
            is SelectionUiEvent.DeleteSelected -> {
                deleteManager.deleteSelected(selectionManager.selectedIds)
            }

            is SelectionUiEvent.ToggleSelectAll -> {
                selectionManager.toggleSelectAll(tableData.value.displayedItems.map { it.id }
                    .toSet())
            }

            is SelectionUiEvent.ToggleSelection -> {
                selectionManager.toggleSelection(event.id)
            }
        }
    }

    fun updateFilters(filters: Map<C, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<C>?) {
        sortManager.update(sort)
    }
}

fun <BD : Any, UI : Any> RequestResult<List<BD>>.toItemListState(mapper: BD.() -> UI): ItemListState1<UI> {
    return when (this) {
        is RequestResult.Error -> ItemListState1.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState1.Loading()
        is RequestResult.Initial -> ItemListState1.Loading()
        is RequestResult.Success<List<BD>> -> ItemListState1.Success(data.map(mapper))
    }
}
