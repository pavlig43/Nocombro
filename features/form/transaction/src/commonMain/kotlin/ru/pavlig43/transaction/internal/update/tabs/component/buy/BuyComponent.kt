package ru.pavlig43.transaction.internal.update.tabs.component.buy

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateComponent
import ru.pavlig43.core.emptyDate
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.buy.BuyBDIn
import ru.pavlig43.database.data.transaction.buy.BuyBDOut
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductDeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.immutable.internal.component.items.productDeclaration.ProductDeclarationTableUi
import ru.pavlig43.mutable.api.multiLine.component.MutableTableComponent
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent.UpdateItem
import ru.pavlig43.mutable.api.singleLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec

internal class BuyComponent(
    componentComponent: ComponentContext,
    private val transactionId: Int,
    private val tabOpener: TabOpener,
    private val immutableTableDependencies: ImmutableTableDependencies,
    repository: UpdateCollectionRepository<BuyBDOut, BuyBDIn>,

    ) : MutableTableComponent<BuyBDOut, BuyBDIn, BuyUi, BuyField>(
    componentContext = componentComponent,
    parentId = transactionId,
    title = "Покупка",
    sortMatcher = BuySorter,
    filterMatcher = BuyFilterMatcher,
    repository = repository
) {

    private val dialogNavigation = SlotNavigation<BuyDialog>()

    internal val dialog = childSlot(
        source = dialogNavigation,
        key = "buy_dialog",
        serializer = BuyDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild,
    )
    @Suppress("LongMethod")
    private fun createDialogChild(dialogConfig: BuyDialog, context: ComponentContext): DialogChild {
        return when (dialogConfig) {
            is BuyDialog.DateBorn -> {
                val item = itemList.value.first { it.composeId == dialogConfig.composeId }
                val dateComponent = DateComponent(
                    componentContext = context,
                    initDate = item.dateBorn,
                    onDismissRequest = {dialogNavigation.dismiss()},
                    onChangeDate = { onEvent(UpdateItem(item.copy(dateBorn = it))) }
                )
                DialogChild.Date(dateComponent)
            }

            is BuyDialog.Declaration -> DialogChild.ImmutableMBS(
                MBSImmutableTableComponent<ProductDeclarationTableUi>(
                    componentContext = context,
                    onDismissed = dialogNavigation::dismiss,
                    onCreate = { tabOpener.openDeclarationTab(0) },
                    dependencies = immutableTableDependencies,
                    immutableTableBuilderData = ProductDeclarationImmutableTableBuilder(
                        productId = dialogConfig.productId,
                    ),
                    onItemClick = { declaration ->
                        val buyUi = itemList.value.first { it.composeId == dialogConfig.composeId }
                        onEvent(
                            UpdateItem(
                                buyUi.copy(
                                    declarationId = declaration.composeId,
                                    declarationName = declaration.displayName,
                                    vendorName = declaration.vendorName
                                )
                            )
                        )
                        dialogNavigation.dismiss()
                    },
                )
            )

            is BuyDialog.Product -> DialogChild.ImmutableMBS(
                MBSImmutableTableComponent<ProductTableUi>(
                    componentContext = context,
                    onDismissed = dialogNavigation::dismiss,
                    onCreate = { tabOpener.openProductTab(0) },
                    dependencies = immutableTableDependencies,
                    immutableTableBuilderData = ProductImmutableTableBuilder(
                        fullListProductTypes = ProductType.entries,
                        withCheckbox = false
                    ),
                    onItemClick = { product ->
                        val buyUi = itemList.value.first { it.composeId == dialogConfig.composeId }
                        onEvent(
                            UpdateItem(
                                buyUi.copy(
                                    productId = product.composeId,
                                    productName = product.displayName,
                                )
                            )
                        )
                        dialogNavigation.dismiss()
                    },
                )
            )
        }
    }

    override val columns: ImmutableList<ColumnSpec<BuyUi, BuyField, TableData<BuyUi>>> =
        createBuyColumn(
            onOpenProductDialog = { dialogNavigation.activate(BuyDialog.Product(it)) },
            onOpenDeclarationDialog = { composeId, productId ->
                dialogNavigation.activate(
                    BuyDialog.Declaration(
                        composeId, productId
                    )
                )
            },
            onOpenDateDialog = {dialogNavigation.activate(BuyDialog.DateBorn(it))},
            onEvent = ::onEvent
        )

    override fun createNewItem(composeId: Int): BuyUi {
        return BuyUi(
            composeId = composeId,
            id = 0
        )
    }

    override fun BuyBDOut.toUi(composeId: Int): BuyUi {
        return BuyUi(
            composeId = composeId,
            productName = productName,
            count = count,
            declarationName = declarationName,
            vendorName = vendorName,
            dateBorn = dateBorn,
            price = price,
            comment = comment,
            id = id
        )
    }

    override fun BuyUi.toBDIn(): BuyBDIn {
        return BuyBDIn(
            count = count,
            transactionId = transactionId,
            dateBorn = dateBorn,
            price = price,
            comment = comment,
            id = id,
            productId = productId,
            declarationId = declarationId
        )
    }

    override val errorMessages: Flow<List<String>> = itemList.map {lst->
        buildList {
            lst.forEach { buyUi ->
                val place = "В строке ${buyUi.composeId + 1}"
                if (buyUi.productId == 0)  add("$place не указан продукт")
                if (buyUi.count == 0) add("$place количество равно 0")
                if (buyUi.declarationId == 0) add("$place нет декларации")
                if (buyUi.dateBorn == emptyDate) add(("$place не выбрана дата"))
                if (buyUi.price == 0) add(("$place не выбрана цена"))
            }
        }
    }

}

@Serializable
internal sealed interface BuyDialog {

    @Serializable
    data class Product(val composeId: Int) : BuyDialog

    @Serializable
    data class Declaration(val composeId: Int, val productId: Int) : BuyDialog

    data class DateBorn(val composeId: Int) : BuyDialog
}

sealed interface DialogChild {
    class ImmutableMBS(val component: MBSImmutableTableComponent<*>) : DialogChild
    class Date(val component: DateComponent) : DialogChild
}