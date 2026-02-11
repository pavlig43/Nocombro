package ru.pavlig43.immutable.internal.component.items.productDeclaration

import ru.pavlig43.tablecore.model.IMultiLineTableUi


data class ProductDeclarationTableUi(
    override val composeId: Int,
    val productId: Int,
    val displayName: String,
    val vendorName: String,
    val isActual: Boolean
): IMultiLineTableUi