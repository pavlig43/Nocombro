package ru.pavlig43.mutable.api.singleLine.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.model.SingleItem
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.tablecore.model.ISingleLineTableUi
import ua.wwind.table.ColumnSpec


data class SingleLineComponentFactory<I : SingleItem, T : ISingleLineTableUi>(
    val initItem: T,
    val errorFactory: (T) -> List<String>,
    val mapperToUi: I.() -> T,
)

abstract class SingleLineComponent<I : SingleItem, UI : ISingleLineTableUi, C>(
    componentContext: ComponentContext,
    private val componentFactory: SingleLineComponentFactory<I, UI>,
    getInitData: (suspend () -> Result<I>)?,
    private val observeOnItem: (UI) -> Unit = {}
) : ComponentContext by componentContext {
    protected val coroutineScope = componentCoroutineScope()

    abstract val columns: ImmutableList<ColumnSpec<UI, C, Unit>>

    private val _itemFields = MutableStateFlow(listOf(componentFactory.initItem))
    val itemFields = _itemFields.asStateFlow()

    val initDataComponent = LoadInitDataComponent<UI>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData?.invoke()?.map { item ->
                componentFactory.mapperToUi(item)
            } ?: Result.success(componentFactory.initItem)

        },
        onSuccessGetInitData = { item ->
            observeOnItem(item)
            _itemFields.update { listOf(item) }
        }
    )


    fun onChangeItem(item: UI) {
        _itemFields.update { listOf(item) }
        observeOnItem(item)

    }

    val errorTableMessages = _itemFields.map { lst ->
        componentFactory.errorFactory(lst[0])
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        emptyList()
    )
}
