package ru.pavlig43.declaration.internal.create.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable
import ru.pavlig43.core.DateComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.internal.DeclarationField
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.declaration.internal.model.toDto
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.VendorImmutableTableBuilder
import ru.pavlig43.immutable.internal.component.items.vendor.VendorTableUi
import ru.pavlig43.mutable.api.singleLine.component.CreateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ua.wwind.table.ColumnSpec

@Suppress("LongParameterList")
internal class CreateDeclarationSingleLineComponent(
    componentContext: ComponentContext,
    onSuccessCreate: (Int) -> Unit,
    observeOnItem: (DeclarationEssentialsUi) -> Unit,
    componentFactory: SingleLineComponentFactory<Declaration, DeclarationEssentialsUi>,
    createDeclarationRepository: CreateSingleItemRepository<Declaration>,
    private val immutableDependencies: ImmutableTableDependencies,
    private val tabOpener: TabOpener,
) : CreateSingleLineComponent<Declaration, DeclarationEssentialsUi, DeclarationField>(
    componentContext = componentContext,
    onSuccessCreate = onSuccessCreate,
    componentFactory = componentFactory,
    createSingleItemRepository = createDeclarationRepository,
    mapperToDTO = { toDto() },
    observeOnItem = observeOnItem
) {
    private val dialogNavigation = SlotNavigation<DialogConfig>()

    val dialog: Value<ChildSlot<DialogConfig, DialogChild>> = childSlot(
        source = dialogNavigation,
        key = "dialog",
        serializer = DialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild
    )

    private fun dialogChild(config: DialogConfig, context: ComponentContext): DialogChild {
        return when (config) {
            DialogConfig.BestBefore -> {
                DialogChild.Date(
                    DateComponent(
                        componentContext = context,
                        initDate = item.value.bestBefore,
                        onChangeDate = { newDate ->
                            onChangeItem1 { it.copy(bestBefore = newDate) }
                        },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }

            DialogConfig.Born -> {
                val item = item.value
                DialogChild.Date(
                    DateComponent(
                        componentContext = context,
                        initDate = item.bornDate,
                        onChangeDate = { newDate -> onChangeItem1 { it.copy(bornDate = newDate) } },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }

            DialogConfig.Vendor -> DialogChild.Vendor(
                MBSImmutableTableComponent<VendorTableUi>(
                    componentContext = context,
                    immutableTableBuilderData = VendorImmutableTableBuilder(false),
                    onItemClick = { vendor ->
                        onChangeItem1 {
                            it.copy(vendorId = vendor.composeId, vendorName = vendor.displayName)
                        }
                        dialogNavigation.dismiss()
                    },
                    onCreate = { tabOpener.openVendorTab(0) },
                    dependencies = immutableDependencies,
                    onDismissed = { dialogNavigation.dismiss() }
                )
            )
        }
    }

    override val columns: ImmutableList<ColumnSpec<DeclarationEssentialsUi, DeclarationField, Unit>> =
        createDeclarationColumns0(
            onOpenVendorDialog = { dialogNavigation.activate(DialogConfig.Vendor) },
            onOpenBornDateDialog = { dialogNavigation.activate(DialogConfig.Born) },
            onOpenBestBeforeDialog = { dialogNavigation.activate(DialogConfig.BestBefore) },
            onChangeItem = ::onChangeItem1
        )
}

@Serializable
internal sealed interface DialogConfig {

    @Serializable
    data object Vendor : DialogConfig

    @Serializable
    data object Born : DialogConfig

    @Serializable
    data object BestBefore : DialogConfig
}

internal sealed interface DialogChild {
    class Date(val component: DateComponent) : DialogChild
    class Vendor(val component: MBSImmutableTableComponent<VendorTableUi>) : DialogChild
}
