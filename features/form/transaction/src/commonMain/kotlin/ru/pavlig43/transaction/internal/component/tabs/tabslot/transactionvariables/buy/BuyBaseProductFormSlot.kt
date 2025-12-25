package ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.ItemEssentialsUi
import ru.pavlig43.database.data.transaction.buy.BuyBaseBD
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent

class BuyBaseProductFormSlot(
    componentContext: ComponentContext,
) : ComponentContext by componentContext, BuyFormSlot {
    override val title: String = "Таблица"


    override suspend fun onUpdate(): Result<Unit> {
        TODO("Not yet implemented")
    }
}

data class BuyBaseUi(
    val composeKey: Int,
    val batchId: Int,
    val productName: String,
    val declarationName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val price: Int,
    val comment: String,
)



abstract class BuyBaseComponent(
    componentContext: ComponentContext,
//    private val componentFactory: EssentialComponentFactory<I, T>,
    getInitData: (suspend () -> Result<List<BuyBaseBD>>)?
) : ComponentContext by componentContext {
    protected val coroutineScope = componentCoroutineScope()

    private val _itemFields = MutableStateFlow<List<BuyBaseUi>>(emptyList())
    val itemFields = _itemFields.asStateFlow()

    val initDataComponent = LoadInitDataComponent<List<BuyBaseUi>>(
        componentContext = childContext("init"),
        getInitData = {
            getInitData?.invoke()?.map { items: List<BuyBaseBD> ->
                items.toUi()
            }?:Result.success(emptyList())

        },
        onSuccessGetInitData = { item ->
            _itemFields.update { item }
        }
    )


    fun onChangeItem(item: List<BuyBaseUi>) {
        _itemFields.update { item }

    }
//TODO
    val isValidFields = _itemFields.map { item ->
        true
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false
    )

}
private fun List<BuyBaseBD>.toUi():List<BuyBaseUi>{
    TODO()

}

sealed interface BuyBaseOnEvent {
    /** Toggle selection for a  item by ID */
    data class ToggleSelection(
        val id: Int,
    ) : BuyBaseOnEvent

    /** Toggle selection for all displayed items */
    data object ToggleSelectAll : BuyBaseOnEvent

    /** Delete all selected items */
    data object DeleteSelected : BuyBaseOnEvent

    /** Clear all selections */
    data object ClearSelection : BuyBaseOnEvent
}