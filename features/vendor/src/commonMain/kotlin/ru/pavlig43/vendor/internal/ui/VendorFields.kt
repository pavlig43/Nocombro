package ru.pavlig43.vendor.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.manageitem.internal.ui.core_field_block.CommentFieldBlock
import ru.pavlig43.manageitem.internal.ui.core_field_block.NameFieldBlock
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi

@Composable
internal fun VendorFields(
    vendor: VendorEssentialsUi,
    updateVendor: (VendorEssentialsUi) -> Unit,
) {
    NameFieldBlock(
        vendor.displayName,
        { updateVendor(vendor.copy(displayName = it)) }
    )

    CommentFieldBlock(
        vendor.comment,
        { updateVendor(vendor.copy(comment = it)) }
    )

}