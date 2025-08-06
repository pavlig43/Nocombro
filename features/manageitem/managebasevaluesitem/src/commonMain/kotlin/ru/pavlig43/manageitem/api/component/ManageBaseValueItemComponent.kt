package ru.pavlig43.manageitem.api.component


import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.Item
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.api.data.RequireValues


class ManageBaseValueItemComponent<I : Item, S : ItemType>(
    componentContext: ComponentContext,
    typeVariantList: List<S>,
    initDataRepository: IInitDataRepository<I, RequireValues>,
    id: Int = 0
) : ComponentContext by componentContext, IManageBaseValueItemComponent {

    private val coroutineScope = componentCoroutineScope()
    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }
    private val _requiredValues = MutableStateFlow(initDataRepository.iniDataForState)
    override val initComponent: ILoadInitDataComponent<RequireValues> = LoadInitDataComponent(
        componentContext = childContext("initComponent"),
        getInitData = initDataRepository::loadInitData,
        id = id,
        onSuccessGetInitData = { requireValues ->
            _requiredValues.update { requireValues }
        }
    )

    override val requireValues = _requiredValues.asStateFlow()


    override fun onNameChange(name: String) {
        _requiredValues.update { it.copy(name = name) }
    }

    private val _typeVariants = MutableStateFlow<List<ItemType>>(typeVariantList)
    override val typeVariants: StateFlow<List<ItemType>> = _typeVariants.asStateFlow()

    override fun onSelectType(type: ItemType) {
        _requiredValues.update { it.copy(type = type) }
    }

    override val isValidAllValue: Flow<Boolean> = _requiredValues.map { it.type != null }

}







