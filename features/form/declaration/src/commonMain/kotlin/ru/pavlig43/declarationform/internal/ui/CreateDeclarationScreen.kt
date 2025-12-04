package ru.pavlig43.declarationform.internal.ui

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.router.slot.child
import ru.pavlig43.declarationform.internal.component.CreateDeclarationComponent
import ru.pavlig43.itemlist.api.ui.MBSItemList
import ru.pavlig43.create.ui.CreateEssentialsScreen

@Composable
internal fun CreateDeclarationScreen(
    component: CreateDeclarationComponent
) {

    CreateEssentialsScreen(component) { item, onItemChange ->
        DeclarationFields(
            declaration = item,
            updateDeclaration = onItemChange,
            onOpenVendorDialog = { component.vendorDialogComponent.showDialog() }
        )
    }
    component.vendorDialogComponent.dialog.child?.instance?.also {
        MBSItemList(it)
    }
}