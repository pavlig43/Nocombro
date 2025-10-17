package ru.pavlig43.productform.internal

import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.manageitem.api.data.DefaultRequireValues
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun DefaultRequireValues.toProduct(): Product {
    return Product(
        displayName = name,
        type = type as ProductType,
        createdAt = createdAt?: Clock.System.now().toEpochMilliseconds(),
        comment = comment,
        id = id
    )
}