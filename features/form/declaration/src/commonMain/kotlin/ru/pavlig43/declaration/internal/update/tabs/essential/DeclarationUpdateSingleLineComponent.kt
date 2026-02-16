package ru.pavlig43.declaration.internal.update.tabs.essential

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.Flow
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
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.mutable.api.singleLine.component.UpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ua.wwind.table.ColumnSpec

@Suppress("LongParameterList")
internal class DeclarationUpdateSingleLineComponent(
    componentContext: ComponentContext,
    declarationId: Int,
    updateRepository: UpdateSingleLineRepository<Declaration>,
    componentFactory: SingleLineComponentFactory<Declaration, DeclarationEssentialsUi>,
    observeOnItem: (DeclarationEssentialsUi) -> Unit,
    onSuccessInitData: (DeclarationEssentialsUi) -> Unit,
    private val immutableDependencies: ImmutableTableDependencies,
    private val tabOpener: TabOpener,
) : UpdateSingleLineComponent<Declaration, DeclarationEssentialsUi, DeclarationField>(
    componentContext = componentContext,
    id = declarationId,
    updateSingleLineRepository = updateRepository,
    componentFactory = componentFactory,
    observeOnItem = observeOnItem,
    onSuccessInitData = onSuccessInitData,
    mapperToDTO = { toDto() }
) {

    override val columns: ImmutableList<ColumnSpec<DeclarationEssentialsUi, DeclarationField, Unit>> =
        createDeclarationColumns1(
            onOpenVendorDialog = { dialogNavigation.activate(UpdateDialogConfig.Vendor) },
            onOpenBornDateDialog = { dialogNavigation.activate(UpdateDialogConfig.Born) },
            onOpenBestBeforeDialog = { dialogNavigation.activate(UpdateDialogConfig.BestBefore) },
            onChangeItem = { item -> onChangeItem(item) }
        )

    private val dialogNavigation = SlotNavigation<UpdateDialogConfig>()

    val dialog: Value<ChildSlot<UpdateDialogConfig, UpdateDialogChild>> = childSlot(
        source = dialogNavigation,
        key = "dialog",
        serializer = UpdateDialogConfig.serializer(),
        handleBackButton = true,
        childFactory = ::dialogChild
    )

    private fun dialogChild(config: UpdateDialogConfig, context: ComponentContext): UpdateDialogChild {
        return when (config) {
            UpdateDialogConfig.BestBefore -> {
                val item = itemFields.value[0]
                UpdateDialogChild.Date(
                    DateComponent(
                        componentContext = context,
                        initDate = item.bestBefore,
                        onChangeDate = { newDate -> onChangeItem(item.copy(bestBefore = newDate)) },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }
            UpdateDialogConfig.Born -> {
                val item = itemFields.value[0]
                UpdateDialogChild.Date(
                    DateComponent(
                        componentContext = context,
                        initDate = item.bornDate,
                        onChangeDate = { newDate -> onChangeItem(item.copy(bornDate = newDate)) },
                        onDismissRequest = { dialogNavigation.dismiss() }
                    )
                )
            }
            UpdateDialogConfig.Vendor -> UpdateDialogChild.Vendor(
                MBSImmutableTableComponent<VendorTableUi>(
                    componentContext = context,
                    immutableTableBuilderData = VendorImmutableTableBuilder(false),
                    onItemClick = { vendor ->
                        val item = itemFields.value[0]
                        onChangeItem(item.copy(vendorId = vendor.composeId, vendorName = vendor.displayName))
                        dialogNavigation.dismiss()
                    },
                    onCreate = { tabOpener.openVendorTab(0) },
                    dependencies = immutableDependencies,
                    onDismissed = { dialogNavigation.dismiss() }
                )
            )
        }
    }

    override val errorMessages: Flow<List<String>> = errorTableMessages
}

@Serializable
internal sealed interface UpdateDialogConfig {

    @Serializable
    data object Vendor : UpdateDialogConfig

    @Serializable
    data object Born : UpdateDialogConfig

    @Serializable
    data object BestBefore : UpdateDialogConfig
}

internal sealed interface UpdateDialogChild {
    class Date(val component: DateComponent) : UpdateDialogChild
    class Vendor(val component: MBSImmutableTableComponent<VendorTableUi>) : UpdateDialogChild
}
