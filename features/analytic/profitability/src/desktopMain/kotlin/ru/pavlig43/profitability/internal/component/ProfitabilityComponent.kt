package ru.pavlig43.profitability.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.datetime.period.dateTime.DateTimePeriodComponent
import ru.pavlig43.profitability.api.ProfitabilityDependencies
import ru.pavlig43.profitability.internal.di.ProfitabilityRepository
import ru.pavlig43.profitability.internal.di.createModule
import ru.pavlig43.profitability.internal.model.AllProfitability
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.tablecore.manger.FilterManager
import ru.pavlig43.tablecore.manger.SortManager
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

class ProfitabilityComponent(
    componentContext: ComponentContext,
    dependencies: ProfitabilityDependencies
) : ComponentContext by componentContext, MainTabComponent {

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Прибыльность"))
    override val model: StateFlow<MainTabComponent.NavTabState> = _model.asStateFlow()

    val dateTimePeriodComponent = DateTimePeriodComponent(
        componentContext = childContext("date_time"),
        initDTPeriod = DTPeriod.thisMonth
    )
    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(createModule(dependencies))
    private val repository: ProfitabilityRepository = scope.get()

    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<ProfitabilityField>(childContext("filter"))
    private val sortManager = SortManager<ProfitabilityField>(childContext("sort"))

    private val _expandedProducts = MutableSetFlow<Int>()

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val loadState: StateFlow<LoadState> = dateTimePeriodComponent.dateTimePeriodForData
        .transformLatest { dateTimePeriod ->
            emit(LoadState.Loading)
            repository.observeOnProducts(
                start = dateTimePeriod.start,
                end = dateTimePeriod.end
            ).collect { result ->
                emit(
                    result.fold(
                        onSuccess = {
                            LoadState.Success(it) },
                        onFailure = { LoadState.Error(it.message ?: "") }
                    )
                )
            }
        }
        .stateIn(coroutineScope, SharingStarted.Lazily, LoadState.Loading)

    internal val tableData: StateFlow<ProfitabilityTableData> = combine(
        loadState,
        filterManager.filters,
        sortManager.sort,
        _expandedProducts.asStateFlow()
    ) { state, filters, sort, expandedIds ->
        when (state) {
            is LoadState.Loading, is LoadState.Error -> ProfitabilityTableData()
            is LoadState.Success -> {
                val filtered = state.data.products.filter { item ->
                    ProfitabilityFilterMatcher.matchesItem(item, filters)
                }
                val displayedProducts = ProfitabilitySorter.sort(filtered, sort).map { product ->
                    product.copy(expandedDetails = expandedIds.contains(product.productId))
                }
                ProfitabilityTableData(displayedProducts = displayedProducts)
            }
        }
    }.stateIn(coroutineScope, SharingStarted.Lazily, ProfitabilityTableData())

    fun updateFilters(filters: Map<ProfitabilityField, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    fun updateSort(sort: SortState<ProfitabilityField>?) {
        sortManager.update(sort)
    }

    fun onToggleDetailsExpanded(productId: Int) {
        _expandedProducts.update { expandedIds ->
            if (expandedIds.contains(productId)) {
                expandedIds - productId
            } else {
                expandedIds + productId
            }
        }
    }
}

internal sealed interface LoadState {
    data object Loading : LoadState
    data class Error(val message: String) : LoadState
    data class Success(val data: AllProfitability) : LoadState
}

private class MutableSetFlow<T>(
    initial: Set<T> = emptySet()
) {
    private val _state = MutableStateFlow(initial)
    val value: Set<T> get() = _state.value

    fun update(update: (Set<T>) -> Set<T>) {
        _state.value = update(_state.value)
    }

    fun asStateFlow(): StateFlow<Set<T>> = _state.asStateFlow()
}