package ru.pavlig43.product.internal.update.tabs.declaration

import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class FlowProductDeclarationTableUi(
    override val composeId: Int,
    val id: Int,
    val declarationId: Int,
    val declarationName: String,
    val vendorName: String,
    val isActual: Boolean
): IMultiLineTableUi