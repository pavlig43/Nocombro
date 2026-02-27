package ru.pavlig43.storage.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.storage.internal.model.toUi

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

    private val _products = MutableStateFlow<List<StorageProductUi>>(emptyList())

    internal val loadState: StateFlow<LoadState> = storageRepository.observeOnStorageProducts()
        .map { result ->
            result.fold(
                onSuccess = { lst ->
                    _products.value = lst.map { it.toUi() }
                    LoadState.Success(_products.value)
                },
                onFailure = { throwable -> LoadState.Error(throwable.message ?: "") }
            )
        }.stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            LoadState.Loading
        )

    val tableData: StateFlow<StorageTableData> = _products
        .map { products -> StorageTableData(displayedProducts = products) }
        .stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            StorageTableData()
        )

    fun toggleExpand(productId: Int) {
        _products.value = _products.value.map { product ->
            if (product.productId == productId) {
                product.copy(expanded = !product.expanded)
            } else {
                product
            }
        }
    }

}
internal sealed interface LoadState{
    data object Loading: LoadState
    data class Error(val message: String): LoadState
    data class Success(val products:List<StorageProductUi>): LoadState
}
