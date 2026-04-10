package ru.pavlig43.product.internal.update.tabs.safety

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

data class SafetyStockUi(
    val id: Int,
    val productId: Int,
    val reorderPoint: DecimalData3,
    val orderQuantity: DecimalData3,
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
): ISingleLineTableUi

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
        reorderPoint = DecimalData3(reorderPoint),
        orderQuantity = DecimalData3(orderQuantity),
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
