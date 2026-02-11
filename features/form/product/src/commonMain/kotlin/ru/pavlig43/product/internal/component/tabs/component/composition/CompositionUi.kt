package ru.pavlig43.product.internal.component.tabs.component.composition

import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.tablecore.model.IMultiLineTableUi

internal data class CompositionUi(
    override val composeId: Int,
    val id: Int,
    val productId: Int,
    val productName: String,
    val productType: ProductType?,
    val count: Int,
) : IMultiLineTableUi




