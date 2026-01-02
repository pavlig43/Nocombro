package ru.pavlig43.product.internal.component.tabs.tabslot.compositionData

import ru.pavlig43.tablecore.model.ITableUi

internal data class CompositionUi(
    override val composeId: Int,
    val id: Int,
    val productId: Int,
    val productName: String,
    val count: Int,
) : ITableUi




