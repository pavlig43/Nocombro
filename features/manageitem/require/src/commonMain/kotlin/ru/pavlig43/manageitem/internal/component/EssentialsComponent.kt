package ru.pavlig43.manageitem.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.ChangeSet
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.mapTo
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.manageitem.api.data.CreateEssentialsRepository
import ru.pavlig43.manageitem.api.data.UpdateEssentialsRepository
import ru.pavlig43.manageitem.internal.data.ItemEssentialsUi

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

abstract class CreateEssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    val onSuccessCreate: (Int) -> Unit,
     componentFactory: EssentialComponentFactory<I,T>,
    private val createEssentialsRepository: CreateEssentialsRepository<I>,
    private val mapperToDTO: T.() -> I,
) : EssentialsComponent<I, T>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = null,

) {
    private val _createState: MutableStateFlow<CreateState> = MutableStateFlow(CreateState.Init)
    internal val createState = _createState.asStateFlow()

    fun create() {
        coroutineScope.launch(Dispatchers.IO) {
            _createState.update { CreateState.Loading }
            val item = itemFields.value.mapperToDTO()
            val idResult = createEssentialsRepository.createEssential(item)
            _createState.update { idResult.toCreateState() }
        }

    }

}

internal interface CreateState {
    data object Init : CreateState
    data object Loading : CreateState
    data class Success(val id: Int) : CreateState
    data class Error(val message: String) : CreateState
}

private fun RequestResult<Int>.toCreateState(): CreateState {
    return when (this) {
        is RequestResult.Error<*> -> CreateState.Error(this.message ?: "Неизвестная ошибка")
        is RequestResult.InProgress -> CreateState.Loading
        is RequestResult.Initial<*> -> CreateState.Init
        is RequestResult.Success<Int> -> CreateState.Success(data)
    }
}

abstract class UpdateEssentialsComponent<I : GenericItem, T : ItemEssentialsUi>(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<I,T>,
    id: Int,
    private val updateEssentialsRepository: UpdateEssentialsRepository<I>,
    private val mapperToDTO: T.() -> I,
) : EssentialsComponent<I, T>(
    componentContext = componentContext,
    componentFactory = componentFactory,
    getInitData = { updateEssentialsRepository.getInit(id) },
), FormTabSlot {
    override val title: String = "Основная информация"

    override suspend fun onUpdate(): RequestResult<Unit> {
        val old = initDataComponent.firstData.value?.mapperToDTO()
        val new = itemFields.value.mapperToDTO()
        return updateEssentialsRepository.update(ChangeSet(old, new))
    }

}
