package ru.pavlig43.manageitem.internal.ui.item_fields

import androidx.compose.runtime.Composable
import ru.pavlig43.manageitem.internal.data.VendorEssentialsUi
import ru.pavlig43.manageitem.internal.ui.core_field_block.CommentFieldBlock
import ru.pavlig43.manageitem.internal.ui.core_field_block.NameFieldBlock

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
        comment = vendor.comment,
        onChangeComment = { updateVendor(vendor.copy(comment = it)) }
    )
}