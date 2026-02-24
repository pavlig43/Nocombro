package ru.pavlig43.storage.api.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.storage.internal.di.StorageRepository

class StorageComponent(
    componentContext: ComponentContext,
    private val storageRepository: StorageRepository
    
): ComponentContext by componentContext, MainTabComponent{

    private val _model = MutableStateFlow(MainTabComponent.NavTabState("Склад"))
    override val model = _model.asStateFlow()
    private val coroutineScope = componentCoroutineScope()

    internal val loadState: StateFlow<LoadState> = storageRepository.observeOnStorageProducts()
        .map { result ->
            val a: LoadState = result.fold(
                onSuccess = {lst-> LoadState.Success(lst)},
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
    data class Success(val str:List<String>): LoadState
}