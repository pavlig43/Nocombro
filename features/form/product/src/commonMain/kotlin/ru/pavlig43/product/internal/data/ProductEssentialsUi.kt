package ru.pavlig43.product.internal.data

import ru.pavlig43.core.data.ItemEssentialsUi
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal data class ProductEssentialsUi(
    val displayName: String = "",

    val type: ProductType? = null,

    val createdAt: Long? = null,

    val comment:String ="",

    override val id: Int = 0,
): ItemEssentialsUi
internal fun Product.toUi(): ProductEssentialsUi {
    return ProductEssentialsUi(
        displayName = displayName,
        type = type,
        createdAt = createdAt,
        comment = comment,
        id = id
    )
}
@OptIn(ExperimentalTime::class)
internal fun ProductEssentialsUi.toDto(): Product {
    return Product(
        type = type?: throw IllegalArgumentException("product type require") ,
        displayName = displayName,
        createdAt = createdAt?: Clock.System.now().toEpochMilliseconds(),
        comment = comment,
        id = id
    )
}