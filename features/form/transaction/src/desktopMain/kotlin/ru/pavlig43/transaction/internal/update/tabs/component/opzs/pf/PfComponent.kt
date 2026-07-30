package ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.ProductDeclarationImmutableTableBuilder
import ru.pavlig43.immutable.api.component.ProductImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.product.ProductTableUi
import ru.pavlig43.immutable.internal.component.items.productDeclaration.ProductDeclarationTableUi
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.thermallabel.api.component.ThermalLabelDialogComponent
import ru.pavlig43.thermallabel.api.data.ThermalLabelTemplateService

private fun factory(transactionId: Int) = SingleLineComponentFactory<PfBD, PfUi>(
    initItem = PfUi(),
    errorFactory = { item ->
        buildList {
            val place = "Полуфабрикат"
            if (item.productId == 0) add("$place не указан продукт")
            if (item.count.value == 0L) add("$place количество равно 0")
            if (item.declarationId == 0) add("$place нет декларации")
        }
    },
    mapperToUi = { toUi(transactionId) }
)

@Suppress("LongParameterList")
internal class PfComponent(
    componentContext: ComponentContext,
    transactionId: Int,
    private val getDateBorn: () -> LocalDate,
    updateSingleLineRepository: UpdateSingleLineRepository<PfBD>,
    private val tabOpener: TabOpener,
    private val immutableTableDependencies: ImmutableTableDependencies,
    private val thermalLabelTemplateService: ThermalLabelTemplateService,
    observeOnItem: (PfUi) -> Unit,
    onSuccessInitData: (PfUi) -> Unit,
) : UpdateSingleLineComponent<PfBD, PfUi>(
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

    @Suppress("LongMethod")
    private fun createDialogChild(dialogConfig: PfDialog, context: ComponentContext): PfDialogChild {
        return when (dialogConfig) {
            is PfDialog.Declaration -> {
                val item = item.value
                PfDialogChild.ImmutableMBS(
                    MBSImmutableTableComponent<ProductDeclarationTableUi>(
                        componentContext = context,
                        onDismissed = dialogNavigation::dismiss,
                        dependencies = immutableTableDependencies,
                        immutableTableBuilderData = ProductDeclarationImmutableTableBuilder(
                            parentId = item.productId,
                        ),
                        tabOpener = tabOpener,
                        onItemClick = { declaration ->
                            onChangeItem {
                                it.copy(
                                    declarationId = declaration.declarationId,
                                    declarationName = declaration.displayName,
                                    vendorName = declaration.vendorName
                                )
                            }
                            dialogNavigation.dismiss()
                        },
                    )
                )
            }

            is PfDialog.Product -> {
                val item = item.value
                PfDialogChild.ImmutableMBS(
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
                            if (product.composeId != item.productId) {
                                onChangeItem {
                                    it.copy(
                                        productId = product.composeId,
                                        productName = product.displayName,
                                        declarationId = 0,
                                        declarationName = "",
                                        vendorName = ""
                                    )
                                }
                            }

                            dialogNavigation.dismiss()
                        },
                    )
                )
            }

            is PfDialog.Label -> {
                val currentItem = item.value
                PfDialogChild.ThermalLabel(
                    ThermalLabelDialogComponent(
                        componentContext = context,
                        productId = currentItem.productId,
                        productName = currentItem.productName,
                        defaultDate = getCurrentLocalDate(),
                        service = thermalLabelTemplateService,
                        onDismissed = dialogNavigation::dismiss,
                    )
                )
            }
        }
    }

    /** Открывает диалог печати этикетки для выбранного продукта. */
    fun openThermalLabelDialog() {
        if (item.value.productId == 0) return
        dialogNavigation.activate(PfDialog.Label)
    }

    /** Открывает диалог выбора продукта. */
    fun openProductDialog() {
        dialogNavigation.activate(PfDialog.Product)
    }

    /** Открывает выбранный продукт в отдельной вкладке. */
    fun openSelectedProduct() {
        item.value.productId
            .takeIf { it != 0 }
            ?.let(tabOpener::openProductTab)
    }

    /** Открывает диалог выбора декларации для выбранного продукта. */
    fun openDeclarationDialog() {
        if (item.value.productId == 0) return
        dialogNavigation.activate(PfDialog.Declaration)
    }

    /** Открывает выбранную декларацию в отдельной вкладке. */
    fun openSelectedDeclaration() {
        item.value.declarationId
            .takeIf { it != 0 }
            ?.let(tabOpener::openDeclarationTab)
    }

    override val errorMessages: Flow<List<String>> = item.map { item ->
        buildList {
            val place = "Полуфабрикат"
            if (item.productId == 0) add("$place не указан продукт")
            if (item.count.value == 0L) add("$place количество равно 0")
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

    @Serializable
    data object Label : PfDialog
}

sealed interface PfDialogChild {
    class ImmutableMBS(val component: MBSImmutableTableComponent<*>) : PfDialogChild
    class ThermalLabel(val component: ThermalLabelDialogComponent) : PfDialogChild
}
