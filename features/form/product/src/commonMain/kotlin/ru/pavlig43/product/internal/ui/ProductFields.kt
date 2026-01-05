package ru.pavlig43.product.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.coreui.coreFieldBlock.CommentFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.NameFieldBlock
import ru.pavlig43.coreui.coreFieldBlock.ReadWriteItemTypeField
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.product.internal.data.ProductEssentialsUi

@Composable
internal fun ProductFields(
    product: ProductEssentialsUi,
    updateProduct: (ProductEssentialsUi) -> Unit,
) {
    NameFieldBlock(
        product.displayName,
        { updateProduct(product.copy(displayName = it)) }
    )
    ReadWriteItemTypeField(
        readOnly = product.id != 0,
        currentType = product.productType,
        typeVariants = ProductType.entries,
        onChangeType = { updateProduct(product.copy(productType = it)) }
    )

    CommentFieldBlock(
        comment = product.comment,
        onChangeComment = { updateProduct(product.copy(comment = it)) }
    )
}