package ru.pavlig43.manageitem.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.itemlist.api.ui.MBSItemList
import ru.pavlig43.manageitem.internal.component.DeclarationComponent
import ru.pavlig43.manageitem.internal.component.DocumentComponent
import ru.pavlig43.manageitem.internal.component.EssentialsComponent
import ru.pavlig43.manageitem.internal.component.ProductComponent
import ru.pavlig43.manageitem.internal.component.VendorComponent
import ru.pavlig43.manageitem.internal.data.DeclarationEssentialsUi
import ru.pavlig43.manageitem.internal.data.DocumentEssentialsUi
import ru.pavlig43.manageitem.internal.data.ItemEssentialsUi
import ru.pavlig43.manageitem.internal.data.ProductEssentialsUi
import ru.pavlig43.manageitem.internal.data.VendorEssentialsUi
import ru.pavlig43.manageitem.internal.ui.item_fields.DeclarationFields
import ru.pavlig43.manageitem.internal.ui.item_fields.DocumentFields
import ru.pavlig43.manageitem.internal.ui.item_fields.ProductFields
import ru.pavlig43.manageitem.internal.ui.item_fields.VendorFields

@Suppress("UNCHECKED_CAST")
@Composable
internal fun ItemFields(
    component: EssentialsComponent<out GenericItem, out ItemEssentialsUi>,
) {
    val item by component.itemFields.collectAsState()

    when (component) {
        is DeclarationComponent -> {
            val dialog by component.dialog.subscribeAsState()
            DeclarationFields(
                declaration = item as DeclarationEssentialsUi,
                updateDeclaration = { component.onChangeItem(it) },
                onOpenVendorDialog = component::showDialog,
            )
            dialog.child?.instance?.also {
                MBSItemList(it)
            }

        }

        is DocumentComponent ->
            DocumentFields(
                document = item as DocumentEssentialsUi,
                updateDocument = { component.onChangeItem(it) })


        is ProductComponent -> ProductFields(
            product = item as ProductEssentialsUi,
            updateProduct = { component.onChangeItem(it) })

        is VendorComponent -> VendorFields(
            vendor = item as VendorEssentialsUi,
            updateVendor = { component.onChangeItem(it) })

    }

}
