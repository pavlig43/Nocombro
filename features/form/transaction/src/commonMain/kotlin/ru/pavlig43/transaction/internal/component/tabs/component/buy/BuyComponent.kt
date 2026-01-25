package ru.pavlig43.transaction.internal.component.tabs.component.buy

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.buy.BuyBD
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductDeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.immutable.internal.component.items.productDeclaration.ProductDeclarationTableUi
import ru.pavlig43.mutable.api.component.MutableTableComponent
import ru.pavlig43.mutable.api.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.update.data.UpdateCollectionRepository
import ua.wwind.table.ColumnSpec

internal class BuyComponent(
    componentComponent: ComponentContext,
    transactionId: Int,
    tabOpener: TabOpener,
    immutableTableDependencies: ImmutableTableDependencies,
    repository: UpdateCollectionRepository<BuyBD, BuyBD>,

    ) : MutableTableComponent<BuyBD, BuyBD, BuyUi, BuyField>(
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
    ) { dialog, context ->
        when (dialog) {
            is BuyDialog.Declaration -> MBSImmutableTableComponent<ProductDeclarationTableUi>(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                onCreate = { tabOpener.openDeclarationTab(0) },
                dependencies = immutableTableDependencies,
                immutableTableBuilderData = ProductDeclarationImmutableTableBuilder(
                    productId = dialog.productId,
                ),
                onItemClick = { declaration ->
                    val buyUi = itemList.value.first { it.composeId == dialog.composeId }
                    onEvent(
                        MutableUiEvent.UpdateItem(
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

            is BuyDialog.Product -> MBSImmutableTableComponent<ProductTableUi>(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                onCreate = { tabOpener.openProductTab(0) },
                dependencies = immutableTableDependencies,
                immutableTableBuilderData = ProductImmutableTableBuilder(
                    fullListProductTypes = ProductType.entries,
                    withCheckbox = false
                ),
                onItemClick = { product ->
                    val buyUi = itemList.value.first { it.composeId == dialog.composeId }
                    onEvent(
                        MutableUiEvent.UpdateItem(
                            buyUi.copy(
                                productId = product.composeId,
                                productName = product.displayName,
                            )
                        )
                    )
                    dialogNavigation.dismiss()
                },
            )
        }

    }
    override val columns: ImmutableList<ColumnSpec<BuyUi, BuyField, TableData<BuyUi>>> =
        createBuyColumn(
            onOpenProductDialog = { dialogNavigation.activate(BuyDialog.Product(it)) },
            onOpenDeclarationDialog = { composeId, productId ->
                dialogNavigation.activate(
                    BuyDialog.Declaration(
                        composeId,productId
                    )
                )
            },
            onEvent = ::onEvent
        )

    override fun createNewItem(composeId: Int): BuyUi {
        return BuyUi(
            composeId = composeId,
            id = 0
        )
    }

    override fun BuyBD.toUi(composeId: Int): BuyUi {
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

    override fun BuyUi.toBDIn(): BuyBD {
        return BuyBD(
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

    override val errorMessages: Flow<List<String>> = itemList.map { it.map { "" } }

}

@Serializable
internal sealed interface BuyDialog {

    @Serializable
    data class Product(val composeId: Int) : BuyDialog

    @Serializable
    data class Declaration(val composeId: Int, val productId: Int) : BuyDialog
}