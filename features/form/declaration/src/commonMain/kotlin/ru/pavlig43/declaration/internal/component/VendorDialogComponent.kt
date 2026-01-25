package ru.pavlig43.declaration.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import kotlinx.serialization.Serializable
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.VendorImmutableTableBuilder

import ru.pavlig43.immutable.internal.component.items.vendor.VendorTableUi

internal class VendorDialogComponent(
    parentComponentContext: ComponentContext,
    private val onChangeVendor: (Int, String) -> Unit,
    dependencies: ImmutableTableDependencies,
    tabOpener: TabOpener,
) {
    private val dialogNavigation = SlotNavigation<MBSVendorDialogConfig>()

    val dialog =
        parentComponentContext.childSlot(
            source = dialogNavigation,
            key = "vendor_dialog",
            serializer = MBSVendorDialogConfig.serializer(),
            handleBackButton = true,
        ) { _: MBSVendorDialogConfig, context ->
            MBSImmutableTableComponent<VendorTableUi>(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                dependencies = dependencies,
                onCreate = { tabOpener.openVendorTab(0) },
                immutableTableBuilderData = VendorImmutableTableBuilder(
                    withCheckbox = false
                ),
                onItemClick = {
                    onChangeVendor(it.composeId, it.displayName)
                    dialogNavigation.dismiss()
                },
            )
        }


    fun showDialog() {
        dialogNavigation.activate(MBSVendorDialogConfig)
    }
}
@Serializable
internal data object MBSVendorDialogConfig