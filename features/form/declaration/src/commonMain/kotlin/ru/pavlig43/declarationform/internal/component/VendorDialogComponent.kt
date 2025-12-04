package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.slot.*
import com.arkivanov.decompose.value.Value
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.itemlist.api.VendorListParamProvider
import ru.pavlig43.itemlist.api.component.MBSItemListComponent

internal class VendorDialogComponent(
    parentComponentContext: ComponentContext,
    private val onChangeVendor: (Int, String) -> Unit,
    itemListDependencies: ItemListDependencies,
    onOpenVendorTab: (Int) -> Unit,
) {
    private val dialogNavigation = SlotNavigation<MBSVendorDialog>()

    val dialog: Value<ChildSlot<MBSVendorDialog, MBSItemListComponent>> =
        parentComponentContext.childSlot(
            source = dialogNavigation,
            key = "vendor_dialog",
            serializer = MBSVendorDialog.serializer(),
            handleBackButton = true,
        ) { _: MBSVendorDialog, context ->
            MBSItemListComponent(
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