package ru.pavlig43.itemlist.dynamic.component

import androidx.compose.runtime.mutableStateListOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.mapTo
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.itemlist.internal.ItemFilter
import ru.pavlig43.itemlist.internal.component.DeleteState
import ru.pavlig43.itemlist.internal.component.toDeleteState
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent

//class ProductBatchTabSlot(
//    componentContext: ComponentContext,
//    transactionId: Int,
//    private val updateRepository: UpdateCollectionRepository<ProductBatchTransactionBDOut, ProductBatchTransactionBDIn>,
//) : ComponentContext by componentContext, TransactionFormSlot {
//    override val title: String = "Материалы"
//
//    internal val batchesList = ProductBatchListComponent(
//        componentContext = childContext("declarationList"),
//        getInitData = {updateRepository.getInit(transactionId)},
//    )
//    private val dialogNavigation = SlotNavigation<DialogConfig>()
//
////    internal val dialog = childSlot(
////        source = dialogNavigation,
////        key = "document_dialog",
////        serializer = ru.pavlig43.declaration.api.component.DialogConfig.serializer(),
////        handleBackButton = true,
////    ) { _, context ->
////        MBSItemListComponent(
////            componentContext = context,
////            onDismissed = dialogNavigation::dismiss,
////            onCreate = { openDeclarationTab(0) },
////            itemListDependencies = itemListDependencies,
////            itemListParamProvider = DeclarationListParamProvider(withCheckbox = false),
////            onItemClick = {dec ->
////                batchesList.addDeclaration(dec as DeclarationItemUi)
////                dialogNavigation.dismiss()
////            },
////        )
////    }
////
////    private fun showDialog() {
////        dialogNavigation.activate(ru.pavlig43.declaration.api.component.DialogConfig)
////    }
////
////    internal fun openDialog() {
////        showDialog()
////    }
//
//
//    override suspend fun onUpdate(): RequestResult<Unit> {
//        TODO("Not yet implemented")
//    }
//}


internal class DynamicListComponent<BDOut: GenericItem,UI: IItemUi>(
    componentContext: ComponentContext,
    private val getInitData: suspend () -> RequestResult<List<BDOut>>,
    private val generateEmptyUi:(composeKey:Int)-> UI,
    private val mapper: BDOut.() -> UI,
    val searchText: StateFlow<ItemFilter.SearchText>,
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()


    private val _items = MutableStateFlow<List<UI>>(emptyList())
    val items = _items.asStateFlow()
    fun actionInSelectedItemIds(checked: Boolean, id: Int) {
        if (checked) {
            _selectedItemIds.add(id)
        } else {
            _selectedItemIds.remove(id)
        }
    }
    val loadInitDataComponent =
        LoadInitDataComponent<List<UI>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().mapTo{ lst-> lst.map(mapper)} },
            onSuccessGetInitData = { items -> _items.update { items } }
        )

    private val _selectedItemIds = mutableStateListOf<Int>()
    val selectedItemIds: List<Int>
        get() = _selectedItemIds
    fun clearSelectedIds() {
        _selectedItemIds.clear()
    }
    fun removeRows(composeKeys: List<Int>) {
        updateList { lst ->
            lst.removeIf { it.composeKey in composeKeys }
            lst
        }
    }


    fun add(row: UI) {
        val composeKey = _items.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val row = generateEmptyUi(composeKey)

        updateList { lst ->
            lst.add(row)
            lst
        }
    }

    fun changeRow(updatedRow: UI) {
        updateList { lst ->
            val index = lst.indexOfFirst { it.composeKey == updatedRow.composeKey }
            if (index != -1 ){
                lst[index] = updatedRow
            }
            lst
        }
    }


    private fun updateList(updateAction: (MutableList<UI>) -> List<UI>) {
        val updatedBatches = _items.value.toMutableList()
        _items.update { updateAction(updatedBatches) }
    }
    val deleteState: MutableStateFlow<DeleteState> =
        MutableStateFlow(DeleteState.Initial())
    fun deleteItems() {
        coroutineScope.launch {
            deleteState.update { DeleteState.Loading() }
            val state = removeRows(_selectedItemIds).run { RequestResult.Success(Unit) }.toDeleteState()
            deleteState.update { state }
            if (state is DeleteState.Success) {
                clearSelectedIds()
            }
            deleteState.update { DeleteState.Initial() }
        }

    }



}

@Serializable
internal data object DialogConfig

//data class ProductBatchUi(
//    val id: Int,
//    val composeKey: Int,
//    val productId: Int? = null,
//    val productName: String? = null,
//    val dateBorn: Long? = null,
//    val batch: Int? = null,
//    val declarationId: Int? = null,
//    val declarationName: String? = null,
//    val vendorName: String? = null,
//)
//
//private fun List<ProductBatchTransactionBDOut>.toUi(): List<ProductBatchUi> {
//    return this.mapIndexed { index, out -> out.toUi(index) }
//}
//
//private fun ProductBatchTransactionBDOut.toUi(composeKey: Int): ProductBatchUi {
//    return ProductBatchUi(
//        productId = productId,
//        productName = productName,
//        dateBorn = dateBorn,
//        batch = batch,
//        declarationId = declarationId,
//        declarationName = declarationName,
//        vendorName = vendorName,
//        id = id,
//        composeKey = composeKey
//    )
//}
//private fun <D : GenericDeclarationOut> List<D>.toListDeclarationUi(): List<ItemDeclarationUi> {
//    return this.mapIndexed { ind, declaration -> declaration.toDeclarationUi(ind) }
//}
//
//
//@OptIn(ExperimentalTime::class)
//private fun <D : GenericDeclarationOut> D.toDeclarationUi(composeKey: Int): ItemDeclarationUi {
//    return ItemDeclarationUi(
//        id = id,
//        declarationId = declarationId,
//        isActual = bestBefore > getUTCNow(),
//        composeKey = composeKey,
//        declarationName = declarationName,
//        vendorName = vendorName,
//        bestBefore = bestBefore
//    )
//}