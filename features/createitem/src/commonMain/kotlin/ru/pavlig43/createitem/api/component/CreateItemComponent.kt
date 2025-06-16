package ru.pavlig43.createitem.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.createitem.api.data.ICreateItemRepository
import ru.pavlig43.createitem.api.data.ValidNameResult
import ru.pavlig43.database.data.common.data.ItemType

class CreateItemComponent<S : ItemType>(
    componentContext: ComponentContext,
    private val repository: ICreateItemRepository<S>
) : ComponentContext by componentContext, ICreateItemComponent {
    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val _name = MutableStateFlow("")
    override val name: StateFlow<String> = _name.asStateFlow()
    override fun onNameChange(name: String) {
        _name.update { name }
    }

    private val _type: MutableStateFlow<S?> = MutableStateFlow(null)
    override val type: StateFlow<ItemType?> = _type.asStateFlow()

    override val isValidName: StateFlow<ValidNameState> = _name.map {
        repository.isValidName(it).toValidNameState()
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = ValidNameState.Initial()
    )

//    private val scope: Scope =
//        koinContext.getOrCreateKoinScope()


}

private fun ValidNameResult.toValidNameState(): ValidNameState {
    return when (this) {
        is ValidNameResult.AllReadyExists -> ValidNameState.AllReadyExists(this.message)
        is ValidNameResult.Empty -> ValidNameState.Empty(this.message)
        is ValidNameResult.Error -> ValidNameState.Error(this.message)
        is ValidNameResult.Valid -> ValidNameState.Valid()
    }
}

