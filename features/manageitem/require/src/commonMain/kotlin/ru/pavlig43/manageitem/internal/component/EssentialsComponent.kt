package ru.pavlig43.manageitem.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.mapTo
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.internal.data.ItemEssentialsUi

abstract class EssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    getInitData: (suspend () -> RequestResult<I>)?,
    private val upsertEssential: suspend (I) -> Unit,
    initItem: T,
    isValidValuesFactory: T.() -> Boolean,
    private val mapperToDTO: T.() -> I,
    private val mapperToUi: I.() -> T,
    private val vendorInfoForTabName: (T) -> Unit,
) : ComponentContext by componentContext {

    private val _itemFields = MutableStateFlow(initItem)
    val itemFields = _itemFields.asStateFlow()

    val initDataComponent = LoadInitDataComponent<T>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData?.invoke()?.mapTo { item -> item.mapperToUi() } ?: RequestResult.Success(
                initItem
            )

        },
        onSuccessGetInitData = { item ->
            _itemFields.update { item }
        }
    )


    fun onChangeItem(item: T) {
        vendorInfoForTabName(item)
        _itemFields.update { item }

    }

    val isValidFields = _itemFields.map { item ->
        item.isValidValuesFactory()
    }



}

class CreateEssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    private val onSuccessCreate:(Int)-> Unit,
    private val createEssentialsRepository: CreateEssentialsRepository<I>,
    initItem: T,
    isValidValuesFactory: T.() -> Boolean,
    mapperToDTO: T.() -> I,
    mapperToUi: I.() -> T,
    vendorInfoForTabName: (T) -> Unit,
) : EssentialsComponent<I, T>(
    componentContext = componentContext,
    getInitData = null,
    upsertEssential = {createEssentialsRepository.createEssential(it)},
    initItem = initItem,
    isValidValuesFactory = isValidValuesFactory,
    mapperToDTO = mapperToDTO,
    mapperToUi = mapperToUi,
    vendorInfoForTabName = vendorInfoForTabName,
){
    private val _upsertState: MutableStateFlow<UpsertState> = MutableStateFlow(UpsertState.Init)
    internal val createState = _upsertState.asStateFlow()

    suspend fun create(item:I) {
        _upsertState.update { UpsertState.Loading }
        val idResult = createEssentialsRepository.createEssential(item)
        _upsertState.update { idResult.toCreateState() }
    }

}
internal interface UpsertState {
    data object Init : UpsertState
    data object Loading : UpsertState
    data class Success(val id: Int): UpsertState
    data class Error(val message: String): UpsertState
}
private fun RequestResult<Int>.toCreateState(): UpsertState {
    return when (this) {
        is RequestResult.Error<*> -> UpsertState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> UpsertState.Loading
        is RequestResult.Initial<*> -> UpsertState.Init
        is RequestResult.Success<Int> -> UpsertState.Success(data)
    }
}
