package ru.pavlig43.declarationform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.core.DateFieldKind
import ru.pavlig43.coreui.coreFieldBlock.datetime.DateFieldBlock
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
        dateTime = declaration.bestBefore,
        onSelectDate = { updateDeclaration(declaration.copy(bestBefore = it)) },
        dateName = "Истекает",
        dateFieldKind = DateFieldKind.Date
    )
    LabelCheckBoxFieldBlock(
        checked = declaration.isObserveFromNotification,
        onChangeChecked = {updateDeclaration(declaration.copy(isObserveFromNotification = it))},
        label = "Отслеживать в оповещениях"
    )

}