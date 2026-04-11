package ru.pavlig43.product.internal.update.tabs.declaration

import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class ProductDeclarationTableUi(
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val isProductInDeclaration: Boolean,
    val isParsing: Boolean,
    val isActual: Boolean
): IMultiLineTableUi{
    override val composeId = declarationId
}
