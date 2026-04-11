package ru.pavlig43.storage.api.component.storage

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.datetime.period.dateTime.DateTimePeriodComponent
import ru.pavlig43.datetime.single.datetime.DateTimeComponent
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.manger.FilterManager
import ua.wwind.table.filter.data.TableFilterState

class StorageComponent(
    componentContext: ComponentContext,
    dependencies: StorageDependencies,
    private val tabOpener: TabOpener,

) : ComponentContext by componentContext, MainTabComponent {

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(
        createStorageModule(dependencies)
    )
    private val storageRepository: StorageRepository = scope.get()

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Склад"))
    override val model = _model.asStateFlow()
    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<StorageProductField>(childContext("filter"))

    internal val dTPeriodComponent = DateTimePeriodComponent(
        componentContext = childContext("date_time_period"),
        initDTPeriod = DTPeriod.now
    )


    private val _products = MutableStateFlow<List<StorageProductUi>>(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    internal val loadState: StateFlow<LoadState> = dTPeriodComponent.dateTimePeriodForData
        .transformLatest { dateTimePeriod ->
            emit(LoadState.Loading)
            storageRepository.observeOnStorageProducts(
                start = dateTimePeriod.start,
                end = dateTimePeriod.end
            )
                .map { result ->
                    result.fold(
                        onSuccess = { lst ->
                            _products.update { lst.toUi() }
                            LoadState.Success
                        },
                        onFailure = { throwable -> LoadState.Error(throwable.message ?: "") }
                    )
                }
                .collect { emit(it) }
        }
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            LoadState.Loading
        )

    internal val tableData: StateFlow<StorageTableData> = combine(
        _products,
        filterManager.filters,
    ) { products, filters ->
        val expandedProductIds = products
            .filter { it.isProduct && it.isExpanded }
            .map { it.productId }
            .toSet()

        val filtered = products.filter { item ->
            val matchesFilter = StorageFilterMatcher.matchesItem(item, filters)
            val hasNegativeValues = item.balanceBeforeStart < 0 ||
                                    item.incoming < 0 ||
                                    item.outgoing < 0 ||
                                    item.balanceOnEnd < 0
            val isVisible = when {
                item.isProduct -> true
                hasNegativeValues -> true  // Показывать партии с отрицательными значениями
                else -> item.productId in expandedProductIds
            }
            matchesFilter && isVisible
        }
        StorageTableData(displayedProducts = filtered)
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        StorageTableData()
    )

    fun toggleExpand(productId: Int) {
        _products.value = _products.value.map { product ->
            if (product.productId == productId && product.isProduct) {
                product.copy(isExpanded = !product.isExpanded)
            } else {
                product
            }
        }
    }

    fun updateFilters(filters: Map<StorageProductField, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    internal fun onRowClick(item: StorageProductUi) {
        when {
            item.isProduct -> {
                // При клике на продукт можно открыть форму продукта (опционально)
                // tabOpener.openProductTab(item.productId)
            }
            else -> {
                // При клике на партию открываем таблицу движений партии
                tabOpener.openBatchMovementTab(
                    batchId = item.itemId,
                    productName = item.productName,
                    start = dTPeriodComponent.dateTimePeriodForData.value.start,
                    end = dTPeriodComponent.dateTimePeriodForData.value.end
                )
            }
        }
    }

}

internal sealed interface LoadState {
    data object Loading : LoadState
    data class Error(val message: String) : LoadState
    data object Success : LoadState
}

@Suppress("UnusedPrivateMember")
private fun List<StorageProduct>.toUi(): List<StorageProductUi> {
    return flatMap { it.toUi() }
}
@Suppress("UnusedPrivateMember")
private fun StorageProduct.toUi(): List<StorageProductUi> {
    val productItem = StorageProductUi(
        productId = productId,
        itemId = productId,
        productName = productName,
        itemName = productName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd,
        isProduct = true,
    )

    val batchItems = batches.map { batch ->
        StorageProductUi(
            productId = productId,
            itemId = batch.batchId,
            productName = this.productName,
            itemName = batch.batchName,
            balanceBeforeStart = batch.balanceBeforeStart,
            incoming = batch.incoming,
            outgoing = batch.outgoing,
            balanceOnEnd = batch.balanceOnEnd,
            isProduct = false,
        )
    }

    return listOf(productItem) + batchItems
}



@Serializable
internal sealed interface StorageDialog {
    @Serializable
    data object StartDateTime : StorageDialog

    @Serializable
    data object EndDateTime : StorageDialog
}

sealed interface DialogChild {
    class DateTime(val component: DateTimeComponent) : DialogChild
}
