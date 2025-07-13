package ru.pavlig43.createitem.api.component


import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.createitem.api.data.IItemFormRepository
import ru.pavlig43.createitem.api.data.ValidNameResult
import ru.pavlig43.database.data.common.data.Item
import ru.pavlig43.database.data.common.data.ItemType

class CreateItemComponent<I: Item,S : ItemType>(
    componentContext: ComponentContext,
    typeVariantList: List<S>,
    private val repository: IItemFormRepository<I,S>
) : ComponentContext by componentContext, ICreateItemComponent {
    private val coroutineScope = componentCoroutineScope()
//    private val koinContext = instanceKeeper.getOrCreate {
//        ComponentKoinContext()
//    }
    private val _name = MutableStateFlow("")
    override val name: StateFlow<String> = _name.asStateFlow()
    override fun onNameChange(name: String) {
        _name.update { name }
    }

    private val _type: MutableStateFlow<S?> = MutableStateFlow(null)
    override val type: StateFlow<ItemType?> = _type.asStateFlow()

    private val _typeVariants = MutableStateFlow<List<ItemType>>(typeVariantList)
    override val typeVariants: StateFlow<List<ItemType>> = _typeVariants.asStateFlow()

    override fun onSelectType(type: ItemType) {
        _type.update { type as S}
    }




    override val isValidName: StateFlow<ValidNameState> = _name.map {
        repository.isValidName(it).toValidNameState()
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.Eagerly,
        initialValue = ValidNameState.Initial()
    )
    override val isValidAllValue: Flow<Boolean> = isValidName.combine(_type){isValidName,type->
        isValidName  is ValidNameState.Valid && type !=null
    }

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

