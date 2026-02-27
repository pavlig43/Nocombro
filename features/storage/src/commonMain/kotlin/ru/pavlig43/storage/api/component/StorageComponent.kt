package ru.pavlig43.storage.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.storage.api.StorageDependencies
import ru.pavlig43.storage.internal.di.StorageRepository
import ru.pavlig43.storage.internal.di.createStorageModule
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

    internal val loadState: StateFlow<LoadState> = storageRepository.observeOnStorageProducts()
        .map { result ->
            val a: LoadState = result.fold(
                onSuccess = {lst-> LoadState.Success(lst.map { it.toUi() })},
                onFailure = {throwable -> LoadState.Error(throwable.message?:"")}
            )
            a
        }.stateIn(
            coroutineScope,
            SharingStarted.Lazily,
            LoadState.Loading
        )

}
internal sealed interface LoadState{
    data object Loading: LoadState
    data class Error(val message: String): LoadState
    data class Success(val str:List<StorageProductUi>): LoadState
}
