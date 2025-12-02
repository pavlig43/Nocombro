package ru.pavlig43.declarationform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.declarationform.internal.data.DeclarationEssentialsUi
import ru.pavlig43.manageitem.internal.ui.core_field_block.BestBeforeFieldBlock
import ru.pavlig43.manageitem.internal.ui.core_field_block.IsObserveNotificationFieldBlock
import ru.pavlig43.manageitem.internal.ui.core_field_block.NameFieldBlock
import ru.pavlig43.manageitem.internal.ui.core_field_block.VendorFieldBlock

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
    BestBeforeFieldBlock(
        date = declaration.bestBefore,
        onSelectDate = { updateDeclaration(declaration.copy(bestBefore = it)) }
    )
    IsObserveNotificationFieldBlock(
        isObserveFromNotification = declaration.isObserveFromNotification,
        onCheckedNotificationVisible = {updateDeclaration(declaration.copy(isObserveFromNotification = it))},
    )

}