package ru.pavlig43.itemlist.core.refac.core.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.collections.immutable.ImmutableList
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
import ru.pavlig43.itemlist.core.refac.api.DocumentBuilder
import ru.pavlig43.itemlist.core.refac.api.BuilderData
import ru.pavlig43.itemlist.core.refac.api.DeclarationBuilder
import ru.pavlig43.itemlist.core.refac.api.ProductBuilder
import ru.pavlig43.itemlist.core.refac.api.TransactionBuilder
import ru.pavlig43.itemlist.core.refac.api.VendorBuilder
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ru.pavlig43.itemlist.core.refac.core.utils.FilterMatcher
import ru.pavlig43.itemlist.core.refac.core.utils.SortMatcher
import ru.pavlig43.itemlist.core.refac.core.model.TableData
import ru.pavlig43.itemlist.core.refac.core.component.manager.DeleteManager
import ru.pavlig43.itemlist.core.refac.core.component.manager.FilterManager
import ru.pavlig43.itemlist.core.refac.core.component.manager.SelectionManager
import ru.pavlig43.itemlist.core.refac.core.component.manager.SortManager
import ru.pavlig43.itemlist.core.refac.core.utils.DefaultFilterMatcher
import ru.pavlig43.itemlist.core.refac.internal.document.DocumentTableComponent
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.statik.internal.component.ImmutableListRepository
import ru.pavlig43.itemlist.statik.internal.di.moduleFactory
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

internal sealed interface ItemListState1<out O> {
    class Loading : ItemListState1<Nothing>
    class Success<O>(val data: List<O>) : ItemListState1<O>
    class Error(val message: String) : ItemListState1<Nothing>
}

object ImmutableTableBuilder{
    @Suppress("UNCHECKED_CAST")
    fun<I: IItemUi> build(
        context: ComponentContext,
        dependencies: ItemStaticListDependencies,
        onCreate: () -> Unit,
        onItemClick:(I)-> Unit,
        builderData: BuilderData<I>): ImmutableTableComponent<*,I, *> {
        return when(builderData){
            is DocumentBuilder -> DocumentTableComponent(
                componentContext = context,
                tableBuilder = builderData,
                onCreate = onCreate,
                onItemClick = onItemClick as (DocumentItemUi) -> Unit,
                dependencies = dependencies
            )

            is DeclarationBuilder -> TODO()
            is ProductBuilder -> TODO()
            is TransactionBuilder -> TODO()
            is VendorBuilder -> TODO()
        }as ImmutableTableComponent<*, I, *>
    }
}

abstract class ImmutableTableComponent<BD, UI : IItemUi, C>(
    componentContext: ComponentContext,
    tableBuilder1: BuilderData<UI>,
    val onCreate: () -> Unit,
    val onItemClick: (UI) -> Unit,
    mapper: BD.() -> UI,
    filterMatcher: DefaultFilterMatcher<UI, C>,
    sortMatcher: SortMatcher<UI, C>,
    dependencies: ItemStaticListDependencies,
) : ComponentContext by componentContext, SlotComponent{

    private val _model = MutableStateFlow(SlotComponent.TabModel(tableBuilder1.tabTitle))
    override val model: StateFlow<SlotComponent.TabModel> = _model.asStateFlow()

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(moduleFactory(dependencies))
    private val repository: ImmutableListRepository<BD> = scope.get()

    internal abstract val columns: ImmutableList<ColumnSpec<UI, C, TableData<UI>>>


    private val coroutineScope = componentCoroutineScope()
    internal val selectionManager = SelectionManager(childContext("selection"))
    internal val filterManager = FilterManager<C>(childContext("filter"))
    internal val sortManager = SortManager<C>(childContext("sort"))

    internal val deleteManager = DeleteManager(
        componentContext = childContext("delete"),
        clearSelection = selectionManager::clearSelected,
        deleteFn = repository::deleteByIds
    )




    internal val itemListState =
        repository.observeOnItems().map {
            it.toItemListState(mapper)
        }.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            ItemListState1.Loading()
        )


    internal val tableData = combine(
        itemListState,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { state, selectedIds, filters, sort ->
        when (state) {
            is ItemListState1.Error,
            is ItemListState1.Loading -> TableData(isSelectionMode = tableBuilder1.withCheckbox)

            is ItemListState1.Success -> {
                val filtered = state.data.filter { ui ->
                    filterMatcher.matchesItem(ui, filters)
                }
                val displayed = sortMatcher.sort(filtered, sort)
                TableData(
                    displayedItems = displayed,
                    selectedIds = selectedIds,
                    isSelectionMode = tableBuilder1.withCheckbox
                )

            }
        }
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = tableBuilder1.withCheckbox)
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


private fun <BD, UI : IItemUi> RequestResult<List<BD>>.toItemListState(mapper: BD.() -> UI): ItemListState1<UI> {
    return when (this) {
        is RequestResult.Error -> ItemListState1.Error(message ?: "unknown error")
        is RequestResult.InProgress -> ItemListState1.Loading()
        is RequestResult.Initial -> ItemListState1.Loading()
        is RequestResult.Success<List<BD>> -> ItemListState1.Success(data.map(mapper))
    }
}
