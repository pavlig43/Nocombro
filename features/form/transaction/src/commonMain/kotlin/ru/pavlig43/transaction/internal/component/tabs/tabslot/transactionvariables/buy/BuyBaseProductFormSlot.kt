package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.emptyDate
import ru.pavlig43.database.data.transaction.buy.BuyBaseBD
import ru.pavlig43.itemlist.api.model.ITableUi
import ru.pavlig43.itemlist.internal.component.SelectionUiEvent
import ru.pavlig43.itemlist.internal.component.manager.FilterManager
import ru.pavlig43.itemlist.internal.component.manager.SelectionManager
import ru.pavlig43.itemlist.internal.component.manager.SortManager
import ru.pavlig43.itemlist.internal.model.TableData
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.component.BuyBaseField
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.component.BuyBaseFilterMatcher
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.component.BuyBaseSorter
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.component.createBuyBaseColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

class BuyBaseProductFormSlot(
    componentContext: ComponentContext,
) : EditableTableComponent(
    componentContext=componentContext,
    getInitData = null
), BuyFormSlot {
    override val title: String = "Таблица"
    override val columns: ImmutableList<ColumnSpec<BuyBaseUi, BuyBaseField, TableData<BuyBaseUi>>> = createBuyBaseColumn(
        onCreate = ::addNew,
        onCallProductDialog = {},
        onEvent = {}
    )


    override suspend fun onUpdate(): Result<Unit> {
        TODO("Not yet implemented")
    }
}

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


abstract class EditableTableComponent(
    componentContext: ComponentContext,
//    private val componentFactory: EssentialComponentFactory<I, T>,
    getInitData: (suspend () -> Result<List<BuyBaseBD>>)?
) : ComponentContext by componentContext {
    protected val coroutineScope = componentCoroutineScope()


    abstract val columns: ImmutableList<ColumnSpec<BuyBaseUi, BuyBaseField, TableData<BuyBaseUi>>>
    private val _itemFields = MutableStateFlow<List<BuyBaseUi>>(emptyList())
    val itemFields = _itemFields.asStateFlow()
    fun addNew(){
        _itemFields.update { lst->
            val composeId = _itemFields.value.maxOfOrNull { it.composeId }?:0
            val newItem = BuyBaseUi(composeId = composeId)
            lst + newItem
        }
    }

    val initDataComponent = LoadInitDataComponent<List<BuyBaseUi>>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData?.invoke()?.map { items: List<BuyBaseBD> ->
                items.toUi()
            } ?: Result.success(emptyList())

        },
        onSuccessGetInitData = { item ->
            _itemFields.update { item }
        }
    )
    val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    val filterManager = FilterManager<BuyBaseField>(childContext("filter"))
    val sortManager = SortManager<BuyBaseField>(childContext("sort"))

    val tableData = combine(
        _itemFields,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { fields, selectedIds, filters, sort ->
        val filtered = fields.filter { ui ->
            BuyBaseFilterMatcher.matchesItem(ui, filters)
        }
        val displayed = BuyBaseSorter.sort(filtered, sort)
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



    fun onChangeItem(item: List<BuyBaseUi>) {
        _itemFields.update { item }

    }

    //TODO
    val isValidFields = _itemFields.map { item ->
        true
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false
    )
    fun updateFilters(filters: Map<BuyBaseField, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<BuyBaseField>?) {
        sortManager.update(sort)
    }
    fun onEvent(event: SelectionUiEvent){

    }

}

private fun List<BuyBaseBD>.toUi(): List<BuyBaseUi> {
    TODO()

}

//sealed interface BuyBaseOnEvent {
//    /** Toggle selection for a  item by ID */
//    data class ToggleSelection(
//        val id: Int,
//    ) : BuyBaseOnEvent
//
//    /** Toggle selection for all displayed items */
//    data object ToggleSelectAll : BuyBaseOnEvent
//
//    /** Delete all selected items */
//    data object DeleteSelected : BuyBaseOnEvent
//
//    /** Clear all selections */
//    data object ClearSelection : BuyBaseOnEvent
//}