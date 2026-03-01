package ru.pavlig43.storage.api.component

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
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.api.column.StorageProductField
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.manger.FilterManager
import ru.pavlig43.tablecore.manger.SortManager
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState
import kotlin.collections.map

class StorageComponent(
    componentContext: ComponentContext,
    dependencies: StorageDependencies

): ComponentContext by componentContext, MainTabComponent{

    private val koinComponent = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinComponent.getOrCreateKoinScope(
        createStorageModule(dependencies)
    )
    private val storageRepository: StorageRepository = scope.get()

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Склад"))
    override val model = _model.asStateFlow()
    private val coroutineScope = componentCoroutineScope()

    private val filterManager = FilterManager<StorageProductField>(childContext("filter"))
    private val sortManager = SortManager<StorageProductField>(childContext("sort"))

    private val _products = MutableStateFlow<List<StorageProductUi>>(emptyList())

    internal val loadState: StateFlow<LoadState> = storageRepository.observeOnStorageProducts()
        .map { result ->
            result.fold(
                onSuccess = { lst ->
                    _products.value = lst.toUi()
                    LoadState.Success(_products.value)
                },
                onFailure = { throwable -> LoadState.Error(throwable.message ?: "") }
            )
        }.stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            LoadState.Loading
        )

    internal val tableData: StateFlow<StorageTableData> = combine(
        _products,
        filterManager.filters,
        sortManager.sort
    ) { products, filters, sort ->
        val filtered = products.filter { item ->
            StorageFilterMatcher.matchesItem(item, filters)
        }
        val displayed = StorageSorter.sort(filtered, sort)
        StorageTableData(displayedProducts = displayed)
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        StorageTableData()
    )

    fun toggleExpand(productId: Int) {
        _products.value = _products.value.map { product ->
            if (product.itemId == productId) {
                product.copy(expanded = !product.expanded)
            } else {
                product
            }
        }
    }

    fun updateFilters(filters: Map<StorageProductField, TableFilterState<*>>) {
        filterManager.update(filters)
    }

    fun updateSort(sort: SortState<StorageProductField>?) {
        sortManager.update(sort)
    }

}
internal sealed interface LoadState{
    data object Loading: LoadState
    data class Error(val message: String): LoadState
    data class Success(val products:List<StorageProductUi>): LoadState
}

private fun List<StorageProduct>.toUi(): List<StorageProductUi> {
    return flatMap { it.toUi() }
}
private fun StorageProduct.toUi(): List<StorageProductUi> {
    val productItem = StorageProductUi(
        productId = productId,
        itemId = productId,
        name = productName,
        balanceBeforeStart = balanceBeforeStart,
        incoming = incoming,
        outgoing = outgoing,
        balanceOnEnd = balanceOnEnd,
        isProduct = true,
        expanded = false
    )

    val batchItems = batches.map { batch ->
        StorageProductUi(
            productId = productId,
            itemId = batch.batchId,
            name = batch.batchName,
            balanceBeforeStart = batch.balanceBeforeStart,
            incoming = batch.incoming,
            outgoing = batch.outgoing,
            balanceOnEnd = batch.balanceOnEnd,
            isProduct = false,
            expanded = false
        )
    }

    return listOf(productItem) + batchItems
}
