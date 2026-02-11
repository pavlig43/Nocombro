package ru.pavlig43.product.internal.model

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.tablecore.model.ISingleLineTableUi
import kotlin.time.ExperimentalTime

internal data class ProductEssentialsUi(
    val displayName: String = "",

    val productType: ProductType? = null,

    val createdAt: LocalDate = getCurrentLocalDate(),

    val comment: String = "",

    val id: Int = 0,
) : ISingleLineTableUi

@OptIn(ExperimentalTime::class)
internal fun ProductEssentialsUi.toDto(): Product {
    return Product(
        type = productType ?: throw IllegalArgumentException("product type require"),
        displayName = displayName,
        createdAt = createdAt,
        comment = comment,
        id = id
    )
}

internal fun Product.toUi(): ProductEssentialsUi {
    return ProductEssentialsUi(
        displayName = displayName,
        productType = type,
        createdAt = createdAt,
        comment = comment,
        id = id
    )
}
