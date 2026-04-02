package ru.pavlig43.product.internal.model

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi
import kotlin.time.ExperimentalTime

internal data class ProductEssentialsUi(
    val displayName: String = "",

    val secondName: String = "",

    val productType: ProductType? = null,

    val createdAt: LocalDate = getCurrentLocalDate(),

    val comment: String = "",

    val priceForSale: DecimalData2 = DecimalData2(0),

    val shelfLifeDays: Int = 0,

    val id: Int = 0,
) : ISingleLineTableUi

@OptIn(ExperimentalTime::class)
internal fun ProductEssentialsUi.toDto(): Product {
    return Product(
        type = productType ?: throw IllegalArgumentException("product type require"),
        displayName = displayName,
        secondName = secondName,
        createdAt = createdAt,
        comment = comment,
        priceForSale = priceForSale.value,
        shelfLifeDays = shelfLifeDays,
        id = id
    )
}

internal fun Product.toUi(): ProductEssentialsUi {
    return ProductEssentialsUi(
        displayName = displayName,
        secondName = secondName,
        productType = type,
        createdAt = createdAt,
        comment = comment,
        priceForSale = DecimalData2(priceForSale),
        shelfLifeDays = shelfLifeDays,
        id = id
    )
}
