package ru.pavlig43.database.data.sync.mirror

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.TransactionType

/**
 * Общий сериализуемый контракт строки typed mirror.
 *
 * [syncId] является стабильной identity между установками и никогда не заменяется
 * локальным Room `id`. [updatedAt] задает версию активного payload, а [deletedAt]
 * превращает строку в tombstone. Для сравнения используется более поздняя из этих
 * дат через [versionAt].
 */
@Serializable
sealed interface MirrorSyncRow {
    val syncId: String
    val updatedAt: LocalDateTime
    val deletedAt: LocalDateTime?
}

/**
 * Сравнивает только переносимое между устройствами содержимое строки.
 *
 * [FileMirrorRow.path] хранит локальный абсолютный путь и закономерно отличается
 * на разных устройствах. Каноническую ссылку на бинарный объект задаёт
 * [FileMirrorRow.remoteObjectKey], поэтому локальный путь не создаёт sync-конфликт.
 */
internal fun MirrorSyncRow.hasSameSyncContent(other: MirrorSyncRow): Boolean = when {
    this is FileMirrorRow && other is FileMirrorRow -> copy(path = other.path) == other
    else -> this == other
}

@Serializable
data class VendorMirrorRow(
    override val syncId: String,
    val displayName: String,
    val comment: String,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class DocumentMirrorRow(
    override val syncId: String,
    val displayName: String,
    val type: DocumentType,
    val createdAt: LocalDate,
    val comment: String,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class DeclarationMirrorRow(
    override val syncId: String,
    val displayName: String,
    val createdAt: LocalDate,
    val vendorSyncId: String,
    val vendorName: String,
    val bornDate: LocalDate,
    val bestBefore: LocalDate,
    val observeFromNotification: Boolean,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ProductMirrorRow(
    override val syncId: String,
    val type: ProductType,
    val displayName: String,
    val secondName: String,
    val createdAt: LocalDate,
    val comment: String,
    val priceForSale: Long,
    val shelfLifeDays: Int,
    val recNds: Int,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ProductSpecificationMirrorRow(
    override val syncId: String,
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
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class SafetyStockMirrorRow(
    override val syncId: String,
    val productSyncId: String,
    val reorderPoint: Long,
    val orderQuantity: Long,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class CompositionMirrorRow(
    override val syncId: String,
    val parentSyncId: String,
    val productSyncId: String,
    val count: Long,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ProductDeclarationMirrorRow(
    override val syncId: String,
    val productSyncId: String,
    val declarationSyncId: String,
    val isProductInDeclaration: Boolean,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class BatchMirrorRow(
    override val syncId: String,
    val productSyncId: String,
    val dateBorn: LocalDate,
    val declarationSyncId: String,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class BatchCostPriceMirrorRow(
    override val syncId: String,
    val batchSyncId: String,
    val costPricePerUnit: Long,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class BatchMovementMirrorRow(
    override val syncId: String,
    val batchSyncId: String,
    val movementType: MovementType,
    val count: Long,
    val transactionSyncId: String,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class BuyMirrorRow(
    override val syncId: String,
    val transactionSyncId: String,
    val movementSyncId: String,
    val price: Long,
    val comment: String,
    val ndsPercent: Int,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class SaleMirrorRow(
    override val syncId: String,
    val transactionSyncId: String,
    val movementSyncId: String,
    val price: Long,
    val comment: String,
    val clientSyncId: String,
    val ndsPercent: Int,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ReminderMirrorRow(
    override val syncId: String,
    val transactionSyncId: String,
    val text: String,
    val reminderDateTime: LocalDateTime,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ExpenseMirrorRow(
    override val syncId: String,
    val transactionSyncId: String? = null,
    val expenseType: ExpenseType,
    val amount: Long,
    val expenseDateTime: LocalDateTime,
    val comment: String,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ExperimentMirrorRow(
    override val syncId: String,
    val title: String,
    val ideaDescription: String,
    val isArchived: Boolean,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ExperimentEntryMirrorRow(
    override val syncId: String,
    val experimentSyncId: String,
    val entryDate: LocalDate,
    val createdAt: LocalDateTime,
    val content: String,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class ExperimentReminderMirrorRow(
    override val syncId: String,
    val experimentSyncId: String,
    val text: String,
    val reminderDateTime: LocalDateTime,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class TransactionMirrorRow(
    override val syncId: String,
    val transactionType: TransactionType,
    val createdAt: LocalDateTime,
    val comment: String,
    val isCompleted: Boolean,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow

@Serializable
data class FileMirrorRow(
    override val syncId: String,
    val ownerType: OwnerType,
    val ownerSyncId: String,
    val displayName: String,
    val path: String,
    val remoteObjectKey: String? = null,
    val remoteStorageProvider: String? = null,
    override val updatedAt: LocalDateTime,
    override val deletedAt: LocalDateTime? = null,
) : MirrorSyncRow
