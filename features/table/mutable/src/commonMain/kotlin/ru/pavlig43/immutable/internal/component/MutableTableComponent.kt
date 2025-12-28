package ru.pavlig43.immutable.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.emptyDate
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.tablecore.manger.FilterManager
import ru.pavlig43.tablecore.manger.SelectionManager
import ru.pavlig43.tablecore.manger.SortManager
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.utils.FilterMatcher
import ru.pavlig43.tablecore.utils.SortMatcher

import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState




data class BuyBaseUi(
    override val composeId: Int,
    val productName: String = "",
    val batchId: Int? = null,
    val count: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val dateBorn: LocalDate = emptyDate,
    val price: Int = 0,
    val comment: String = "",
) : ITableUi


internal abstract class MutableTableComponent<BD, UI : ITableUi, C,E: MutableUiEvent>(
    componentContext: ComponentContext,
//    private val componentFactory: EssentialComponentFactory<I, T>,
    getInitData: suspend () -> Result<List<BD>>,

    ) : ComponentContext by componentContext {
    protected val coroutineScope = componentCoroutineScope()


    internal abstract val columns: ImmutableList<ColumnSpec<UI, C, TableData<UI>>>
    protected abstract fun createNewItem(composeId: Int): UI
    protected abstract fun BD.toUi(composeId: Int): UI
    protected abstract val filterMatcher: FilterMatcher<UI,C>
    protected abstract val sortMatcher: SortMatcher<UI,C>
    private val _itemList = MutableStateFlow<List<UI>>(emptyList())
    val itemList = _itemList.asStateFlow()
    fun addNew() {
        _itemList.update { lst ->
            val composeId = _itemList.value.maxOfOrNull { it.composeId } ?: 0
            val newItem = createNewItem(composeId)
            lst + newItem
        }
    }

    val initDataComponent = LoadInitDataComponent<List<UI>>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData().map { lst ->
                lst.mapIndexed { index, bd -> bd.toUi(index) }
            }
        },
        onSuccessGetInitData = { lst ->
            _itemList.update { lst }
        }
    )
    val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    val filterManager = FilterManager<C>(childContext("filter"))
    val sortManager = SortManager<C>(childContext("sort"))

    val tableData = combine(
        _itemList,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { fields, selectedIds, filters, sort ->
        val filtered = fields.filter { ui ->
           filterMatcher.matchesItem(ui, filters)
        }
        val displayed = sortMatcher.sort(filtered, sort)
        TableData(
            displayedItems = displayed,
            selectedIds = selectedIds,
            isSelectionMode = true
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = true)
    )


    fun onChangeItem(item: List<UI>) {
        _itemList.update { item }

    }

    //TODO
    val isValidFields = _itemList.map { item ->
        true
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false
    )

    fun updateFilters(filters: Map<C, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<C>?) {
        sortManager.update(sort)
    }

    internal abstract fun onEvent(event: MutableUiEvent)

}






