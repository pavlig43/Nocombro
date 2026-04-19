package ru.pavlig43.database.data.sync

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.TransactionType

const val CURRENT_SYNC_PAYLOAD_VERSION = 1

@Serializable
data class SyncPayloadEnvelope<T>(
    val version: Int = CURRENT_SYNC_PAYLOAD_VERSION,
    val payload: T,
)

@Serializable
data class VendorSyncPayload(
    val syncId: String,
    val displayName: String,
    val comment: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class DocumentSyncPayload(
    val syncId: String,
    val displayName: String,
    val type: DocumentType,
    val createdAt: LocalDate,
    val comment: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class DeclarationSyncPayload(
    val syncId: String,
    val displayName: String,
    val createdAt: LocalDate,
    val vendorSyncId: String,
    val vendorName: String,
    val bornDate: LocalDate,
    val bestBefore: LocalDate,
    val observeFromNotification: Boolean,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class ProductSyncPayload(
    val syncId: String,
    val type: ProductType,
    val displayName: String,
    val secondName: String,
    val createdAt: LocalDate,
    val comment: String,
    val priceForSale: Long,
    val shelfLifeDays: Int,
    val recNds: Int,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class ProductSpecificationSyncPayload(
    val syncId: String,
    val productSyncId: String,
    val dosage: String,
    val composition: String,
    val shelfLifeText: String,
    val storageConditions: String,
    val appearance: String,
    val color: String,
    val smell: String,
    val taste: String,
    val physicalChemicalIndicators: String,
    val microbiologicalIndicators: String,
    val toxicElements: String,
    val allergens: String,
    val gmoInfo: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class SafetyStockSyncPayload(
    val syncId: String,
    val productSyncId: String,
    val reorderPoint: Long,
    val orderQuantity: Long,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class CompositionSyncPayload(
    val syncId: String,
    val parentSyncId: String,
    val productSyncId: String,
    val count: Long,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class ProductDeclarationSyncPayload(
    val syncId: String,
    val productSyncId: String,
    val declarationSyncId: String,
    val isProductInDeclaration: Boolean,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class BatchSyncPayload(
    val syncId: String,
    val productSyncId: String,
    val dateBorn: LocalDate,
    val declarationSyncId: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class BatchMovementSyncPayload(
    val syncId: String,
    val batchSyncId: String,
    val movementType: MovementType,
    val count: Long,
    val transactionSyncId: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class BuySyncPayload(
    val syncId: String,
    val transactionSyncId: String,
    val movementSyncId: String,
    val price: Long,
    val comment: String,
    val ndsPercent: Int,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class SaleSyncPayload(
    val syncId: String,
    val transactionSyncId: String,
    val movementSyncId: String,
    val price: Long,
    val comment: String,
    val clientSyncId: String,
    val ndsPercent: Int,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class ReminderSyncPayload(
    val syncId: String,
    val transactionSyncId: String,
    val text: String,
    val reminderDateTime: LocalDateTime,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class ExpenseSyncPayload(
    val syncId: String,
    val transactionSyncId: String? = null,
    val expenseType: ExpenseType,
    val amount: Long,
    val expenseDateTime: LocalDateTime,
    val comment: String,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class TransactionSyncPayload(
    val syncId: String,
    val transactionType: TransactionType,
    val createdAt: LocalDateTime,
    val comment: String,
    val isCompleted: Boolean,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)

@Serializable
data class FileSyncPayload(
    val syncId: String,
    val ownerType: OwnerType,
    val ownerSyncId: String,
    val displayName: String,
    val path: String,
    val remoteObjectKey: String? = null,
    val remoteStorageProvider: String? = null,
    val updatedAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null,
)
