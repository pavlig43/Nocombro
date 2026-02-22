package ru.pavlig43.immutable.internal.component.items.product

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class ProductTableUi(
    override val composeId: Int,
    val displayName: String,
    val type: ProductType,
    val createdAt: LocalDate,
    val comment: String = "",

    val priceForSale: Int = 0,
) : IMultiLineTableUi