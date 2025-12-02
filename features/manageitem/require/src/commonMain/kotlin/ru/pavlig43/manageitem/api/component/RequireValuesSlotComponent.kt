package ru.pavlig43.manageitem.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.*
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.core.mapTo
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.manageitem.api.data.DefaultRequireValues

class RequireValuesSlotComponent<I : Item, S : ItemType>(
    componentContext: ComponentContext,
    typeVariantList: List<S>,
    /**
     * Передается на самый верх для отображения названия вкладки
     */
    private val onChangeValueForMainTab: (String) -> Unit,
    private val getInitData: (suspend () -> RequestResult<I>)?
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()
    private val _requiredValues = MutableStateFlow(DefaultRequireValues())

     val requireValues = _requiredValues.asStateFlow()


    init {
        _requiredValues.onEach {
            val tabTitle = "${it.name} ${it.type ?: ""}"
            onChangeValueForMainTab(tabTitle)
        }.launchIn(coroutineScope)
    }

     val initComponent: LoadInitDataComponent<DefaultRequireValues> = LoadInitDataComponent(
        componentContext = childContext("initComponent"),
        getInitData = {
            getInitData?.invoke()?.mapTo { it.toRequireValues() }
                ?: RequestResult.Success(
                    DefaultRequireValues()
                )
        },
        onSuccessGetInitData = { requireValues ->
            _requiredValues.update { requireValues }
        }
    )

     fun onNameChange(name: String) {
        _requiredValues.update { it.copy(name = name) }
    }

    private val _typeVariants = MutableStateFlow<List<ItemType>>(typeVariantList)
     val typeVariants: StateFlow<List<ItemType>> = _typeVariants.asStateFlow()


     fun onSelectType(type: ItemType) {
        _requiredValues.update { it.copy(type = type) }
    }

     fun onCommentChange(comment: String) {
        _requiredValues.update { it.copy(comment = comment) }

    }

     val isValidAllValue: Flow<Boolean> = _requiredValues.map { it.type != null }

    private fun Item.toRequireValues(): DefaultRequireValues {
        return DefaultRequireValues(
            id = id,
            name = displayName,
            type = type,
            createdAt = createdAt,
            comment = comment
        )
    }
}


