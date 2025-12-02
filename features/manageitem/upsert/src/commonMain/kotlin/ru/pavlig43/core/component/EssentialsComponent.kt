package ru.pavlig43.core.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.*
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.mapTo
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.core.data.ItemEssentialsUi

data class EssentialComponentFactory<I : GenericItem, T : ItemEssentialsUi>(
    val initItem: T,
    val isValidValuesFactory: T.() -> Boolean,
    val mapperToUi: I.() -> T,
    val vendorInfoForTabName: (T) -> Unit,
)

abstract class EssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    private val componentFactory: EssentialComponentFactory<I,T>,
    getInitData: (suspend () -> RequestResult<I>)?,
) : ComponentContext by componentContext {
    protected val coroutineScope = componentCoroutineScope()

    private val _itemFields = MutableStateFlow(componentFactory.initItem)
    val itemFields = _itemFields.asStateFlow()

    val initDataComponent = LoadInitDataComponent<T>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData?.invoke()?.mapTo { item -> componentFactory.mapperToUi(item) } ?: RequestResult.Success(
                componentFactory.initItem
            )

        },
        onSuccessGetInitData = { item ->
            _itemFields.update { item }
        }
    )


    fun onChangeItem(item: T) {
        componentFactory.vendorInfoForTabName(item)
        _itemFields.update { item }

    }

    val isValidFields = _itemFields.map { item ->
        componentFactory.isValidValuesFactory(item)
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false
    )


}

internal interface CreateState {
    data object Init : CreateState
    data object Loading : CreateState
    data class Success(val id: Int) : CreateState
    data class Error(val message: String) : CreateState
}

internal fun RequestResult<Int>.toCreateState(): CreateState {
    return when (this) {
        is RequestResult.Error<*> -> CreateState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> CreateState.Loading
        is RequestResult.Initial<*> -> CreateState.Init
        is RequestResult.Success<Int> -> CreateState.Success(data)
    }
}

