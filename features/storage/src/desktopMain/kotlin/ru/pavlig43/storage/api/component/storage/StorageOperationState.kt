package ru.pavlig43.storage.api.component.storage

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.transact.StockOperationReason
import ru.pavlig43.storage.internal.model.StorageProductUi

internal data class StorageBatchActionsState(
    val item: StorageProductUi,
    val storageLocation: StorageLocation,
)

internal enum class StorageOperationKind {
    TRANSFER,
    WRITE_OFF,
}

internal data class StorageOperationDialogState(
    val kind: StorageOperationKind,
    val batchId: Int,
    val productName: String,
    val batchName: String,
    val source: StorageLocation,
    val target: StorageLocation?,
    val availableCount: Long,
    val countText: String = "",
    val occurredAt: LocalDateTime,
    val reason: StockOperationReason,
    val comment: String = "",
    val costPricePerUnit: Long? = null,
    val selectedCost: Long = 0,
    val expiryDate: LocalDate? = null,
    val isPreviewLoading: Boolean = true,
    val isSaving: Boolean = false,
    val requiresMissingCostConfirmation: Boolean = false,
    val error: String? = null,
) {
    val count: Long?
        get() = countText.trim().replace(',', '.').toBigDecimalOrNull()
            ?.movePointRight(3)
            ?.let { value -> runCatching { value.longValueExact() }.getOrNull() }
}
