package ru.pavlig43.product.internal.update.tabs.safety

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import ru.pavlig43.mutable.api.input.decimalValue
import ru.pavlig43.mutable.api.input.toDecimalInputText
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

data class SafetyStockUi(
    val id: Int,
    val productId: Int,
    val reorderPointState: TextFieldState = TextFieldState(),
    val orderQuantityState: TextFieldState = TextFieldState(),
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
) : SingleLineInputState {
    val reorderPoint: DecimalData3 get() = DecimalData3(reorderPointState.decimalValue(3) ?: 0L)
    val orderQuantity: DecimalData3 get() = DecimalData3(orderQuantityState.decimalValue(3) ?: 0L)
    override val textFields get() = listOf(reorderPointState, orderQuantityState)
    override val inputErrors get() = if (
        reorderPointState.decimalValue(3) == null ||
        orderQuantityState.decimalValue(3) == null
    ) listOf("Некорректное число") else emptyList()
}

internal fun SafetyStockUi.toDto(): SafetyStock {
    return SafetyStock(
        productId = productId,
        reorderPoint = reorderPoint.value,
        orderQuantity = orderQuantity.value,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
internal fun SafetyStock.toUi(): SafetyStockUi {
    return SafetyStockUi(
        productId = productId,
        reorderPointState = TextFieldState((DecimalData3(reorderPoint)).toDecimalInputText()),
        orderQuantityState = TextFieldState((DecimalData3(orderQuantity)).toDecimalInputText()),
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
