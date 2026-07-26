package ru.pavlig43.immutable.internal.component.items.product

import ru.pavlig43.database.data.product.Product

internal data class ProductTableItem(
    val product: Product,
    val vendorNames: String,
)
