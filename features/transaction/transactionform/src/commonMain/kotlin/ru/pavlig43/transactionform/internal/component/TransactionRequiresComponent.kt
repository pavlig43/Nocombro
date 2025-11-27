package ru.pavlig43.transactionform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.componentCoroutineScope
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.ProductTransactionOut
import ru.pavlig43.database.data.transaction.TransactionRowOut
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.itemlist.api.ProductListParamProvider
import ru.pavlig43.itemlist.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.api.data.IItemUi
import ru.pavlig43.loadinitdata.api.component.ILoadInitDataComponent
import ru.pavlig43.loadinitdata.api.component.LoadInitDataComponent
import ru.pavlig43.transactionform.internal.data.ProductTransactionUi
import ru.pavlig43.transactionform.internal.data.TransactionRowUi

internal class TransactionRequiresComponent(
    componentContext: ComponentContext,
    private val onChangeValueForMainTab: (String) -> Unit,
    private val getInitData: (suspend () -> RequestResult<ProductTransactionOut>)?,
    itemListDependencies: ItemListDependencies,
    openProductTab: (Int) -> Unit,
) : ComponentContext by componentContext {

    private val coroutineScope = componentCoroutineScope()


    val initComponent: ILoadInitDataComponent<ProductTransactionUi> = LoadInitDataComponent(
        componentContext = childContext("initComponent"),
        getInitData = {
            getInitData?.invoke()?.mapTo { it.toProductTransactionUi() } ?: RequestResult.Success(
                ProductTransactionUi()
            )
        },
        onSuccessGetInitData = { requireValues ->
            _transaction.update { requireValues }
        }
    )

    private val _transaction: MutableStateFlow<ProductTransactionUi> =
        MutableStateFlow(ProductTransactionUi())

    internal val transaction = _transaction.asStateFlow()

    init {
        _transaction.onEach {
            val tabTitle = "$it"
            onChangeValueForMainTab(tabTitle)
        }.launchIn(coroutineScope)
    }

    val transactionRowsComponent = TransactionRowsComponent(
        componentContext = childContext("transaction_rows"),
        openProductTab = openProductTab,
        itemListDependencies = itemListDependencies,
        initData = _transaction.value.rows,
    )




}

internal class TransactionRowsComponent(
    componentContext: ComponentContext,
    val openProductTab: (Int) -> Unit,
    itemListDependencies: ItemListDependencies,
    initData: List<TransactionRowUi>,
) : ComponentContext by componentContext {

    private val dialogNavigation = SlotNavigation<MBSProductDialog>()

    internal val dialog: Value<ChildSlot<MBSProductDialog, MBSItemListComponent>> =
        childSlot(
            source = dialogNavigation,
            key = "product_dialog",
            serializer = MBSProductDialog.serializer(),
            handleBackButton = true,
        ) { config: MBSProductDialog, context ->
            MBSItemListComponent(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                onCreate = { openProductTab(0) },
                itemListDependencies = itemListDependencies,
                itemListParamProvider = ProductListParamProvider(
                    fullListProductTypes = ProductType.entries,
                    withCheckbox = false
                ),
                onItemClick = {
                    addTransactionRow(it.id, it.displayName)
                    dialogNavigation.dismiss()
                },
            )
        }

    fun showDialog() {
        dialogNavigation.activate(MBSProductDialog)
    }

    fun removeProductBatchRow(index: Int) {
        updateList { lst ->
            lst.removeIf { it.composeKey == index }
            lst
        }
    }


    fun addTransactionRow(id: Int, displayName: String) {
        val composeKey = _transactionRowUiList.value.maxOfOrNull { it.composeKey }?.plus(1) ?: 0
        val transactionRowUi = TransactionRowUi(
            composeKey = composeKey,
            productId = id,
            displayName = displayName,
            dateBorn = null,
            batchNumber = null,
            id = 0,
            declarationWithVendorName = null
        )

        updateList { lst ->
            lst.add(transactionRowUi)
            lst
        }
    }


    private fun updateList(updateAction: (MutableList<TransactionRowUi>) -> List<TransactionRowUi>) {
        val updatedDeclarations = _transactionRowUiList.value.toMutableList()
        _transactionRowUiList.update { updateAction(updatedDeclarations) }
    }


    private val _transactionRowUiList = MutableStateFlow<List<TransactionRowUi>>(initData)
    val transactionRowsUiList = _transactionRowUiList.asStateFlow()


}

private fun List<TransactionRowOut>.toTransactionRowsUiList(): List<TransactionRowUi> {
    return this.mapIndexed { index, tr ->
        TransactionRowUi(
            composeKey = index,
            productId = tr.productId,
            displayName = tr.productName,
            dateBorn = tr.dateBorn,
            declarationWithVendorName = tr.declarationWithVendorName,
            batchNumber = tr.batch,
            id = tr.id
        )
    }
}


private fun ProductTransactionOut.toProductTransactionUi(): ProductTransactionUi {
    return  ProductTransactionUi(
        transactionType = transaction.transactionType,
        operationType = transaction.operationType,
        date = transaction.date,
        comment = transaction.comment,
        id = transaction.id,
        rows = productRows.toTransactionRowsUiList()
    )
}

@Serializable
internal data object MBSProductDialog
