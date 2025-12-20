package ru.pavlig43.itemlist.internal.component.items.product

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.model.IItemUi

data class ProductItemUi(
    override val id: Int,
    val displayName: String,
    val type: ProductType,
    val createdAt: LocalDate,
    val comment: String = "",
) : IItemUi