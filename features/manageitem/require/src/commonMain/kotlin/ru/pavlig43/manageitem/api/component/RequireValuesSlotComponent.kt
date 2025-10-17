package ru.pavlig43.manageitem.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.core.mapTo
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
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
) : ComponentContext by componentContext, DefaultRequireValuesSlotComponent {

    private val coroutineScope = componentCoroutineScope()
    private val _requiredValues = MutableStateFlow(DefaultRequireValues())

    override val requireValues = _requiredValues.asStateFlow()


    init {
        _requiredValues.onEach {
            val tabTitle = "${it.name} ${it.type ?: ""}"
            onChangeValueForMainTab(tabTitle)
        }.launchIn(coroutineScope)
    }

    override val initComponent: ILoadInitDataComponent<DefaultRequireValues> = LoadInitDataComponent(
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

    override fun onNameChange(name: String) {
        _requiredValues.update { it.copy(name = name) }
    }

    private val _typeVariants = MutableStateFlow<List<ItemType>>(typeVariantList)
    override val typeVariants: StateFlow<List<ItemType>> = _typeVariants.asStateFlow()


    override fun onSelectType(type: ItemType) {
        _requiredValues.update { it.copy(type = type) }
    }

    override fun onCommentChange(comment: String) {
        _requiredValues.update { it.copy(comment = comment) }

    }

    override val isValidAllValue: Flow<Boolean> = _requiredValues.map { it.type != null }

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
