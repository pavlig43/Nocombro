package ru.pavlig43.mutable.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.tablecore.manger.FilterManager
import ru.pavlig43.tablecore.manger.SelectionManager
import ru.pavlig43.tablecore.manger.SortManager
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.utils.FilterMatcher
import ru.pavlig43.tablecore.utils.SortMatcher
import ru.pavlig43.update.data.UpdateCollectionRepository
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState


abstract class MutableTableComponent<BDOut: CollectionObject,BDIn:CollectionObject, UI : ITableUi, C>(
    componentContext: ComponentContext,
    parentId: Int,
    override val title: String,
    sortMatcher: SortMatcher<UI, C>,
    filterMatcher: FilterMatcher<UI, C>,
    private  val repository: UpdateCollectionRepository<BDOut, BDIn>

    ) : ComponentContext by componentContext, FormTabComponent {

    protected val coroutineScope = componentCoroutineScope()



    abstract val columns: ImmutableList<ColumnSpec<UI, C, TableData<UI>>>
    protected abstract fun createNewItem(composeId: Int): UI
    protected abstract fun BDOut.toUi(composeId: Int): UI

    protected abstract fun UI.toBDIn(): BDIn
    private val _itemList = MutableStateFlow<List<UI>>(emptyList())
    val itemList = _itemList.asStateFlow()





    val initDataComponent = LoadInitDataComponent<List<UI>>(
        componentContext = childContext("init"),
        getInitData = {
            repository.getInit(parentId).map { lst ->
                lst.mapIndexed { index, bd -> bd.toUi(index) }
            }
        },
        onSuccessGetInitData = { lst ->
            _itemList.update { lst }
        }
    )
    protected val selectionManager =
        SelectionManager(
            childContext("selection")
        )
    protected val filterManager = FilterManager<C>(childContext("filter"))
    protected val sortManager = SortManager<C>(childContext("sort"))

    val tableData = combine(
        _itemList,
        selectionManager.selectedIdsFlow,
        filterManager.filters,
        sortManager.sort,
    ) { fields, selectedIds, filters, sort, ->
        val filtered = fields.filter { ui ->
            filterMatcher.matchesItem(ui, filters)
        }
        val displayed = sortMatcher.sort(filtered, sort)
        TableData(
            displayedItems = displayed,
            selectedIds = selectedIds,
            isSelectionMode = true,
        )
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        TableData(isSelectionMode = true)
    )

    fun updateFilters(filters: Map<C, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    /** Update sort state - triggers automatic recalculation via StateFlow combination */
    fun updateSort(sort: SortState<C>?) {
        sortManager.update(sort)
    }

    @Suppress("UNCHECKED_CAST")
    fun onEvent(event: MutableUiEvent) {
        when (event) {
            is MutableUiEvent.DeleteSelected -> {
                _itemList.update { lst ->

                    val updatedList = lst - lst.filter { it.composeId in selectionManager.selectedIds }.toSet()
                    selectionManager.clearSelected()
                    updatedList
                }
            }

            is MutableUiEvent.Selection -> {selectionManager.onEvent(event.selectionUiEvent)}
            is MutableUiEvent.UpdateItem -> {
                _itemList.update { lst ->
                    lst.map { ui ->
                        (if (ui.composeId == event.item.composeId) {
                            event.item
                        } else {
                            ui
                        }) as UI
                    }
                }
            }

            MutableUiEvent.CreateNewItem -> {
                _itemList.update { lst ->
                    val composeId = lst.maxOfOrNull { it.composeId }?.plus(1) ?: 0
                    lst + createNewItem(composeId)
                }
            }
        }

    }
    override suspend fun onUpdate(): Result<Unit> {
        val old = initDataComponent.firstData.value?.map { it.toBDIn() }
        val new = _itemList.value.map { it.toBDIn() }
        return repository.update(ChangeSet(old, new))
    }
}






