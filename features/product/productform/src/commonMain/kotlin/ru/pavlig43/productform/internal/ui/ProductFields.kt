package ru.pavlig43.productform.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.manageitem.internal.ui.core_field_block.CommentFieldBlock
import ru.pavlig43.manageitem.internal.ui.core_field_block.ItemTypeField
import ru.pavlig43.manageitem.internal.ui.core_field_block.NameFieldBlock
import ru.pavlig43.productform.internal.data.ProductEssentialsUi

@Composable
internal fun ProductFields(
    product: ProductEssentialsUi,
    updateProduct: (ProductEssentialsUi) -> Unit,
) {
    NameFieldBlock(
        product.displayName,
        { updateProduct(product.copy(displayName = it)) }
    )

    ItemTypeField(
        typeVariants = ProductType.entries,
        currentType = product.type,
        onChangeType = { updateProduct(product.copy(type = it)) }
    )

    CommentFieldBlock(
        comment = product.comment,
        onChangeComment = { updateProduct(product.copy(comment = it)) }
    )
}