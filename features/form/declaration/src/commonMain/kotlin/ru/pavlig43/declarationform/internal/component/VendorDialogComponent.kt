package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.ChildSlot
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.decompose.router.slot.dismiss
import com.arkivanov.decompose.value.Value
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.itemlist.api.VendorListParamProvider
import ru.pavlig43.itemlist.api.component.MBSItemListComponent
import ru.pavlig43.itemlist.internal.component.VendorItemUi

internal class VendorDialogComponent(
    parentComponentContext: ComponentContext,
    private val onChangeVendor: (Int, String) -> Unit,
    itemListDependencies: ItemListDependencies,
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
                itemListDependencies = itemListDependencies,
                onCreate = { onOpenVendorTab(0) },
                itemListParamProvider = VendorListParamProvider(
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