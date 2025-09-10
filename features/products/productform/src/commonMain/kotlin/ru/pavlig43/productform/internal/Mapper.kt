package ru.pavlig43.productform.internal

import ru.pavlig43.core.UTC
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.manageitem.api.data.RequireValues
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun RequireValues.toProduct(): Product {
    return Product(
        displayName = name,
        type = type as ProductType,
        createdAt = createdAt?.value?: Clock.System.now().toEpochMilliseconds(),
        comment = comment,
        id = id
    )
}