package ru.pavlig43.declaration.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.api.component.MBSImmutableTableComponent
import ru.pavlig43.immutable.api.component.VendorImmutableTableBuilder

import ru.pavlig43.immutable.internal.component.items.vendor.VendorTableUi

internal class VendorDialogComponent(
    parentComponentContext: ComponentContext,
    private val onChangeVendor: (Int, String) -> Unit,
    dependencies: ImmutableTableDependencies,
    onOpenVendorTab: (Int) -> Unit,
) {
    private val dialogNavigation = SlotNavigation<MBSVendorDialog>()

    val dialog =
        parentComponentContext.childSlot(
            source = dialogNavigation,
            key = "vendor_dialog",
            serializer = MBSVendorDialog.serializer(),
            handleBackButton = true,
        ) { _: MBSVendorDialog, context ->
            MBSImmutableTableComponent<VendorTableUi>(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                dependencies = dependencies,
                onCreate = { onOpenVendorTab(0) },
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
        dialogNavigation.activate(MBSVendorDialog)
    }
}