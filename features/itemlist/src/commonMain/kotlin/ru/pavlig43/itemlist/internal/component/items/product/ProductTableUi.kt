package ru.pavlig43.itemlist.internal.component.items.product

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.model.ITableUi

data class ProductTableUi(
    override val composeId: Int,
    val displayName: String,
    val type: ProductType,
    val createdAt: LocalDate,
    val comment: String = "",
) : ITableUi