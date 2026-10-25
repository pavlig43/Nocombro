package ru.pavlig43.product.internal.model

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import ru.pavlig43.mutable.api.input.decimalValue
import ru.pavlig43.mutable.api.input.toDecimalInputText
import ru.pavlig43.mutable.api.input.integerValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.getCurrentLocalDate
import kotlin.time.ExperimentalTime

internal data class ProductEssentialsUi(
    val displayNameState: TextFieldState = TextFieldState(),

    val secondNameState: TextFieldState = TextFieldState(),

    val productType: ProductType? = null,

    val createdAt: LocalDate = getCurrentLocalDate(),

    val commentState: TextFieldState = TextFieldState(),

    val priceForSaleState: TextFieldState = TextFieldState(),

    val shelfLifeDaysState: TextFieldState = TextFieldState(),

    val recNdsState: TextFieldState = TextFieldState(),

    val id: Int = 0,

    val syncId: String = defaultSyncId(),

    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    val deletedAt: LocalDateTime? = null,
) : SingleLineInputState {
    val displayName: String get() = displayNameState.text.toString()
    val secondName: String get() = secondNameState.text.toString()
    val comment: String get() = commentState.text.toString()
    val priceForSale: DecimalData2 get() = DecimalData2(priceForSaleState.decimalValue(2) ?: 0L)
    val shelfLifeDays: Int get() = shelfLifeDaysState.integerValue(0..Int.MAX_VALUE) ?: 0
    val recNds: Int get() = recNdsState.integerValue(0..99) ?: 0
    override val textFields get() = listOf(
        displayNameState,
        secondNameState,
        commentState,
        priceForSaleState,
        shelfLifeDaysState,
        recNdsState,
    )
    override val inputErrors get() = if (
        priceForSaleState.decimalValue(2) == null ||
        shelfLifeDaysState.integerValue(0..Int.MAX_VALUE) == null ||
        recNdsState.integerValue(0..99) == null
    ) listOf("Некорректное число") else emptyList()
}

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
        recNds = recNds,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

internal fun Product.toUi(): ProductEssentialsUi {
    return ProductEssentialsUi(
        displayNameState = TextFieldState(displayName),
        secondNameState = TextFieldState(secondName),
        productType = type,
        createdAt = createdAt,
        commentState = TextFieldState(comment),
        priceForSaleState = TextFieldState((DecimalData2(priceForSale)).toDecimalInputText()),
        shelfLifeDaysState = TextFieldState((shelfLifeDays).takeUnless { it == 0 }?.toString().orEmpty()),
        recNdsState = TextFieldState((recNds).takeUnless { it == 0 }?.toString().orEmpty()),
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
