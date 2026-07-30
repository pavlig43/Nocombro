package ru.pavlig43.mutable.api.singleLine.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent

/** Хранит начальную UI-модель, её проверку и перевод из предметной модели. */
data class SingleLineComponentFactory<I : SingleItem, T : Any>(
    val initItem: T,
    val errorFactory: (T) -> List<String>,
    val mapperToUi: I.() -> T,
)

/** Базовый компонент карточной формы с одной записью. */
abstract class SingleLineComponent<I : SingleItem, UI : Any>(
    componentContext: ComponentContext,
    private val componentFactory: SingleLineComponentFactory<I, UI>,
    getInitData: (suspend () -> Result<I>)?,
    private val observeOnItem: (UI) -> Unit,
    onSuccessInitData: (UI) -> Unit = {},
) : ComponentContext by componentContext {
    protected val coroutineScope = componentCoroutineScope()

    private val _item = MutableStateFlow(componentFactory.initItem)

    /** Текущая UI-модель формы. */
    val item = _item.asStateFlow()

    val initDataComponent = LoadInitDataComponent<UI>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData?.invoke()?.map { item ->
                componentFactory.mapperToUi(item)
            } ?: Result.success(componentFactory.initItem)
        },
        onSuccessGetInitData = { item ->
            onSuccessInitData(item)
            _item.update { item }
        },
    )

    /** Меняет UI-модель и сообщает наблюдателю о новом значении. */
    fun onChangeItem(updateItem: (UI) -> UI) {
        val updatedItem = updateItem(item.value)
        _item.update { updatedItem }
        observeOnItem(updatedItem)
    }

    /** Ошибки проверки текущей UI-модели. */
    val validationErrors = item.map(componentFactory.errorFactory).stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        emptyList(),
    )
}
