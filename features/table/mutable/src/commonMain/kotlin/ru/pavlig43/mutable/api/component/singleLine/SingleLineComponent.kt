package ru.pavlig43.mutable.api.component.singleLine

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
import ru.pavlig43.core.model.GenericItem
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.tablecore.model.ITableUi
import ua.wwind.table.ColumnSpec


data class SingleLineComponentFactory<I : GenericItem, T : ITableUi>(
    val initItem: T,
    val isValidFieldsFactory: T.() -> Boolean,
    val mapperToUi: I.() -> T,
    val produceInfoForTabName: (T) -> Unit,
)
abstract class SingleLineComponent<I : GenericItem, UI : ITableUi,C>(
    componentContext: ComponentContext,
    private val componentFactory: SingleLineComponentFactory<I, UI>,
    getInitData: (suspend () -> Result<I>)?,
    private val observeOnEssentials:(UI)-> Unit ={},
    onSuccessInitData:(UI)-> Unit = {}
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
                    .also { componentFactory.produceInfoForTabName(it) }
            }?:Result.success(componentFactory.initItem)

        },
        onSuccessGetInitData = { item ->
            onSuccessInitData(item)
            _itemFields.update { listOf(item) }
        }
    )


    fun onChangeItem(item: UI) {
        componentFactory.produceInfoForTabName(item)
        _itemFields.update { listOf(item) }
        observeOnEssentials(item)

    }

    val isValidFields = _itemFields.map { items ->
        componentFactory.isValidFieldsFactory(items[0])
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false
    )

}
