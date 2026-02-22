package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductDeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.immutable.internal.component.items.productDeclaration.ProductDeclarationTableUi
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec

private fun factory(transactionId: Int) = SingleLineComponentFactory<PfBD, PfUi>(
    initItem = PfUi(),
    errorFactory = { item ->
        buildList {
            val place = "Полуфабрикат"
            if (item.productId == 0) add("$place не указан продукт")
            if (item.count == 0) add("$place количество равно 0")
            if (item.declarationId == 0) add("$place нет декларации")
        }
    },
    mapperToUi = { toUi(transactionId) }
)

@Suppress("LongParameterList")
internal class PfComponent(
    componentContext: ComponentContext,
    transactionId: Int,
    getDateBorn: () -> LocalDate,
    updateSingleLineRepository: UpdateSingleLineRepository<PfBD>,
    private val tabOpener: TabOpener,
    private val immutableTableDependencies: ImmutableTableDependencies,
    observeOnItem: (PfUi) -> Unit = {},
    onSuccessInitData: (PfUi) -> Unit = {},
) : UpdateSingleLineComponent<PfBD, PfUi, PfField>(
    componentContext = componentContext,
    id = transactionId,
    updateSingleLineRepository = updateSingleLineRepository,
    componentFactory = factory(transactionId),
    observeOnItem = observeOnItem,
    onSuccessInitData = onSuccessInitData,
    mapperToDTO = { toDto(getDateBorn) }
) {
    override val title: String
        get() = "ПФ"
    private val dialogNavigation = SlotNavigation<PfDialog>()


    val dialog: Value<ChildSlot<PfDialog, PfDialogChild>> = childSlot(
        source = dialogNavigation,
        key = "pf_dialog",
        serializer = PfDialog.serializer(),
        handleBackButton = true,
        childFactory = ::createDialogChild
    )

    private fun createDialogChild(dialogConfig: PfDialog, context: ComponentContext): PfDialogChild {
        return when (dialogConfig) {
            is PfDialog.Declaration -> {
                val item = itemFields.value[0]
                PfDialogChild.ImmutableMBS(
                    MBSImmutableTableComponent<ProductDeclarationTableUi>(
                        componentContext = context,
                        onDismissed = dialogNavigation::dismiss,
                        onCreate = { tabOpener.openDeclarationTab(0) },
                        dependencies = immutableTableDependencies,
                        immutableTableBuilderData = ProductDeclarationImmutableTableBuilder(
                            parentId = item.productId,
                        ),
                        onItemClick = { declaration ->
                            onChangeItem(
                                item.copy(
                                    declarationId = declaration.declarationId,
                                    declarationName = declaration.displayName,
                                    vendorName = declaration.vendorName
                                )
                            )
                            dialogNavigation.dismiss()
                        },
                    )
                )
            }

            is PfDialog.Product -> {
                val item = itemFields.value[0]
                PfDialogChild.ImmutableMBS(
                    MBSImmutableTableComponent<ProductTableUi>(
                        componentContext = context,
                        onDismissed = dialogNavigation::dismiss,
                        onCreate = { tabOpener.openProductTab(0) },
                        dependencies = immutableTableDependencies,
                        immutableTableBuilderData = ProductImmutableTableBuilder(
                            fullListProductTypes = ProductType.entries - ProductType.FOOD_SALE,
                            withCheckbox = false
                        ),
                        onItemClick = { product ->
                            if (product.composeId != item.productId) {
                                onChangeItem(
                                    item.copy(
                                        productId = product.composeId,
                                        productName = product.displayName,
                                        declarationId = 0,
                                        declarationName = "",
                                        vendorName = ""
                                    )
                                )
                            }

                            dialogNavigation.dismiss()
                        },
                    )
                )
            }
        }
    }

    override val columns: ImmutableList<ColumnSpec<PfUi, PfField, Unit>> =
        createPfColumns(
            onOpenProductDialog = { dialogNavigation.activate(PfDialog.Product) },
            onOpenDeclarationDialog = { dialogNavigation.activate(PfDialog.Declaration) },
            onChangeItem = { item -> onChangeItem(item) }
        )

    override val errorMessages: Flow<List<String>> = item.map { item ->
        buildList {
            val place = "Полуфабрикат"
            if (item.productId == 0) add("$place не указан продукт")
            if (item.count == 0) add("$place количество равно 0")
            if (item.declarationId == 0) add("$place нет декларации")
        }
    }
}

@Serializable
internal sealed interface PfDialog {
    @Serializable
    data object Product : PfDialog

    @Serializable
    data object Declaration : PfDialog
}

sealed interface PfDialogChild {
    class ImmutableMBS(val component: MBSImmutableTableComponent<*>) : PfDialogChild
}
