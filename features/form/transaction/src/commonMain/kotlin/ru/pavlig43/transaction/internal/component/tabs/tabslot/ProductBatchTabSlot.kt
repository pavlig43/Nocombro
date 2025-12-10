package ru.pavlig43.transaction.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.transaction.ProductBatchTransactionBDIn
import ru.pavlig43.database.data.transaction.ProductBatchTransactionBDOut
import ru.pavlig43.itemlist.api.DeclarationListParamProvider
import ru.pavlig43.itemlist.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.internal.component.DeclarationItemUi
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.update.data.UpdateCollectionRepository

class ProductBatchTabSlot(
    componentContext: ComponentContext,
    transactionId: Int,
    private val updateRepository: UpdateCollectionRepository<ProductBatchTransactionBDOut, ProductBatchTransactionBDIn>,
) : ComponentContext by componentContext, TransactionFormSlot {
    override val title: String = "Материалы"

    internal val batchesList = ProductBatchListComponent(
        componentContext = childContext("declarationList"),
        getInitData = {updateRepository.getInit(transactionId)},
    )
    private val dialogNavigation = SlotNavigation<DialogConfig>()

//    internal val dialog = childSlot(
//        source = dialogNavigation,
//        key = "document_dialog",
//        serializer = ru.pavlig43.declaration.api.component.DialogConfig.serializer(),
//        handleBackButton = true,
//    ) { _, context ->
//        MBSItemListComponent(
//            componentContext = context,
//            onDismissed = dialogNavigation::dismiss,
//            onCreate = { openDeclarationTab(0) },
//            itemListDependencies = itemListDependencies,
//            itemListParamProvider = DeclarationListParamProvider(withCheckbox = false),
//            onItemClick = {dec ->
//                batchesList.addDeclaration(dec as DeclarationItemUi)
//                dialogNavigation.dismiss()
//            },
//        )
//    }
//
//    private fun showDialog() {
//        dialogNavigation.activate(ru.pavlig43.declaration.api.component.DialogConfig)
//    }
//
//    internal fun openDialog() {
//        showDialog()
//    }


    override suspend fun onUpdate(): RequestResult<Unit> {
        TODO("Not yet implemented")
    }
}

internal class ProductBatchListComponent(
    componentContext: ComponentContext,
    private val getInitData: suspend () -> RequestResult<List<ProductBatchTransactionBDOut>>,
) : ComponentContext by componentContext {

    fun removeRow(composeKey: Int) {
        updateList { lst: MutableList<ProductBatchUi> ->
            lst.removeIf { it.composeKey == composeKey }
            lst
        }
    }


    fun add(row: ProductBatchUi) {
        val composeKey = _productBatchesRows.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val row = ProductBatchUi(
            id = 0,
            composeKey = composeKey,
        )

        updateList { lst ->
            lst.add(row)
            lst
        }
    }

    fun changeRow(updatedRow: ProductBatchUi) {
        updateList { lst ->
            val index = lst.indexOfFirst { it.composeKey == updatedRow.composeKey }
            if (index != -1 ){
                lst[index] = updatedRow
            }
            lst
        }
    }


    private fun updateList(updateAction: (MutableList<ProductBatchUi>) -> List<ProductBatchUi>) {
        val updatedBatches = _productBatchesRows.value.toMutableList()
        _productBatchesRows.update { updateAction(updatedBatches) }
    }


    private val _productBatchesRows = MutableStateFlow<List<ProductBatchUi>>(emptyList())
    val productBatchesUi = _productBatchesRows.asStateFlow()
    val loadInitDataComponent =
        LoadInitDataComponent<List<ProductBatchUi>>(
            componentContext = childContext("loadInitData"),
            getInitData = { getInitData().mapTo { it.toUi() } },
            onSuccessGetInitData = { batches -> _productBatchesRows.update { batches } }
        )


}

@Serializable
internal data object DialogConfig

data class ProductBatchUi(
    val id: Int,
    val composeKey: Int,
    val productId: Int? = null,
    val productName: String? = null,
    val dateBorn: Long? = null,
    val batch: Int? = null,
    val declarationId: Int? = null,
    val declarationName: String? = null,
    val vendorName: String? = null,
)

private fun List<ProductBatchTransactionBDOut>.toUi(): List<ProductBatchUi> {
    return this.mapIndexed { index, out -> out.toUi(index) }
}

private fun ProductBatchTransactionBDOut.toUi(composeKey: Int): ProductBatchUi {
    return ProductBatchUi(
        productId = productId,
        productName = productName,
        dateBorn = dateBorn,
        batch = batch,
        declarationId = declarationId,
        declarationName = declarationName,
        vendorName = vendorName,
        id = id,
        composeKey = composeKey
    )
}
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