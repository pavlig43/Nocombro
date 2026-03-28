package ru.pavlig43.transaction.internal.update.tabs.component.sale

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.sale.SaleBDOut
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.BatchImmutableTableBuilder
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.api.component.VendorImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.batch.BatchTableUi
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.immutable.internal.component.items.vendor.VendorTableUi
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent.UpdateItem
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class SaleComponent(
    componentComponent: ComponentContext,
    private val transactionId: Int,
    private val tabOpener: TabOpener,
    private val immutableTableDependencies: ImmutableTableDependencies,
    repository: UpdateCollectionRepository<SaleBDOut, SaleBDOut>,

    ) : MutableTableComponent<SaleBDOut, SaleBDOut, SaleUi, SaleField>(
    componentContext = componentComponent,
    parentId = transactionId,
    title = "Продажа",
    sortMatcher = SaleSorter,
    filterMatcher = SaleFilterMatcher,
    repository = repository
) {

    private val dialogNavigation = SlotNavigation<SaleDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "sale_dialog",
        serializer = SaleDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild,
    )

    @Suppress("LongMethod")
    private fun createDialogChild(
        dialogConfig: SaleDialog,
        context: ComponentContext
    ): DialogChild {
        return when (dialogConfig) {
            is SaleDialog.Product -> DialogChild.ImmutableMBS(
                MBSImmutableTableComponent<ProductTableUi>(
                    componentContext = context,
                    onDismissed = dialogNavigation::dismiss,
                    dependencies = immutableTableDependencies,
                    immutableTableBuilderData = ProductImmutableTableBuilder(
                        fullListProductTypes = ProductType.entries,
                        withCheckbox = false
                    ),
                    tabOpener = tabOpener,
                    onItemClick = { product ->
                        val saleUi = itemList.value.first { it.composeId == dialogConfig.composeId }
                        onEvent(
                            UpdateItem(
                                saleUi.copy(
                                    productId = product.composeId,
                                    productName = product.displayName,
                                    price = DecimalData2(product.priceForSale)
                                )
                            )
                        )
                        dialogNavigation.dismiss()
                    },
                )
            )

            is SaleDialog.Batch -> DialogChild.ImmutableMBS(
                MBSImmutableTableComponent<BatchTableUi>(
                    componentContext = context,
                    onDismissed = dialogNavigation::dismiss,
                    dependencies = immutableTableDependencies,
                    immutableTableBuilderData = BatchImmutableTableBuilder(
                        parentId = dialogConfig.productId
                    ),
                    tabOpener = tabOpener,
                    onItemClick = { batch ->
                        val saleUi = itemList.value.first { it.composeId == dialogConfig.composeId }
                        onEvent(
                            UpdateItem(
                                saleUi.copy(
                                    batchId = batch.batchId,
                                    vendorName = batch.vendorName,
                                    dateBorn = batch.dateBorn
                                )
                            )
                        )
                        dialogNavigation.dismiss()
                    },
                )
            )

            is SaleDialog.Client -> DialogChild.ImmutableMBS(
                MBSImmutableTableComponent<VendorTableUi>(
                    componentContext = context,
                    onDismissed = dialogNavigation::dismiss,
                    dependencies = immutableTableDependencies,
                    immutableTableBuilderData = VendorImmutableTableBuilder(
                        withCheckbox = false
                    ),
                    tabOpener = tabOpener,
                    onItemClick = { vendor ->
                        val saleUi = itemList.value.first { it.composeId == dialogConfig.composeId }
                        onEvent(
                            UpdateItem(
                                saleUi.copy(
                                    clientId = vendor.composeId,
                                    clientName = vendor.displayName
                                )
                            )
                        )
                        dialogNavigation.dismiss()
                    },
                )
            )
        }
    }

    override val columns: ImmutableList<ColumnSpec<SaleUi, SaleField, TableData<SaleUi>>> =
        createSaleColumn(
            onOpenProductDialog = { dialogNavigation.activate(SaleDialog.Product(it)) },
            onOpenBatchDialog = { composeId, productId ->
                dialogNavigation.activate(
                    SaleDialog.Batch(
                        composeId, productId
                    )
                )
            },
            onOpenClientDialog = { dialogNavigation.activate(SaleDialog.Client(it)) },
            onEvent = ::onEvent
        )

    override fun createNewItem(composeId: Int): SaleUi {
        return SaleUi(
            composeId = composeId,
            id = 0
        )
    }

    override fun SaleBDOut.toUi(composeId: Int): SaleUi {
        return SaleUi(
            composeId = composeId,
            movementId = movementId,
            batchId = batchId,
            productId = productId,
            productName = productName,
            count = DecimalData3(count),
            vendorName = vendorName,
            dateBorn = dateBorn,
            clientName = clientName,
            clientId = clientId,
            price = DecimalData2(price),
            comment = comment,
            id = id
        )
    }

    override fun SaleUi.toBDIn(): SaleBDOut {
        return SaleBDOut(
            count = count.value,
            transactionId = transactionId,
            dateBorn = dateBorn,
            price = price.value,
            comment = comment,
            id = id,
            productId = productId,
            productName = productName,
            batchId = batchId,
            vendorName = vendorName,
            clientName = clientName,
            clientId = clientId,
            movementId = movementId
        )
    }

    override val errorMessages: Flow<List<String>> = itemList.map { lst ->
        buildList {
            if (lst.map { it.clientId }.toSet().size != 1) {
                add("Клиенты должны быть одинаковы")
            }
            lst.forEach { saleUi ->
                val place = "В строке ${saleUi.composeId + 1}"
                if (saleUi.productId == 0) add("$place не указан продукт")
                if (saleUi.batchId == 0) add("$place не выбрана партия")
                if (saleUi.count.value == 0L) add("$place количество равно 0")
                if (saleUi.clientId == 0) add("$place не выбран клиент")
                if (saleUi.price.value == 0L) add("$place не выбрана цена")
            }
        }
    }

}

@Serializable
internal sealed interface SaleDialog {

    @Serializable
    data class Product(val composeId: Int) : SaleDialog

    @Serializable
    data class Batch(val composeId: Int, val productId: Int) : SaleDialog

    @Serializable
    data class Client(val composeId: Int) : SaleDialog
}

sealed interface DialogChild {
    class ImmutableMBS(val component: MBSImmutableTableComponent<*>) : DialogChild
}
