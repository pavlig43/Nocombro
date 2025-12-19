package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import ru.pavlig43.itemlist.statik.ItemStaticListDependencies
import ru.pavlig43.itemlist.core.refac.api.VendorListParamProvider
import ru.pavlig43.itemlist.statik.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.statik.internal.component.VendorItemUi

internal class VendorDialogComponent(
    parentComponentContext: ComponentContext,
    private val onChangeVendor: (Int, String) -> Unit,
    itemStaticListDependencies: ItemStaticListDependencies,
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
            MBSItemListComponent<VendorItemUi>(
                componentContext = context,
                onDismissed = dialogNavigation::dismiss,
                itemStaticListDependencies = itemStaticListDependencies,
                onCreate = { onOpenVendorTab(0) },
                immutableTableBuilder = VendorListParamProvider(
                    withCheckbox = false
                ),
                onItemClick = {
                    onChangeVendor(it.id, it.displayName)
                    dialogNavigation.dismiss()
                },
            )
        }


    fun showDialog() {
        dialogNavigation.activate(MBSVendorDialog)
    }
}