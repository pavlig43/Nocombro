package ru.pavlig43.declarationform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.coreui.coreFieldBlock.DateFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.LabelCheckBoxFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.NameFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.VendorFieldBlock
import ru.pavlig43.declarationform.internal.data.DeclarationEssentialsUi

@Composable
internal fun DeclarationFields(
    declaration: DeclarationEssentialsUi,
    onOpenVendorDialog:()-> Unit,
    updateDeclaration: (DeclarationEssentialsUi) -> Unit,
) {
    NameFieldBlock(
        declaration.displayName,
        { updateDeclaration(declaration.copy(displayName = it)) }
    )
    VendorFieldBlock(
        declaration.vendorName,
        onOpenVendorDialog = onOpenVendorDialog
    )
    DateFieldBlock(
        date = declaration.bestBefore,
        onSelectDate = { updateDeclaration(declaration.copy(bestBefore = it)) },
        dateName = "Истекает",

    )
    LabelCheckBoxFieldBlock(
        checked = declaration.isObserveFromNotification,
        onChangeChecked = {updateDeclaration(declaration.copy(isObserveFromNotification = it))},
        label = "Отслеживать в оповещениях"
    )

}