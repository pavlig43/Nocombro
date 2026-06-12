package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.experiment.Experiment
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.vendor.Vendor

/**
 * Преобразует корневого контрагента в transport-neutral mirror row.
 *
 * Локальный числовой id намеренно не переносится: identity задается [Vendor.syncId].
 */
internal fun Vendor.toMirrorRow() = VendorMirrorRow(
    syncId = syncId,
    displayName = displayName,
    comment = comment,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** Преобразует корневой документ без локальных Room-ссылок. */
internal fun Document.toMirrorRow() = DocumentMirrorRow(
    syncId = syncId,
    displayName = displayName,
    type = type,
    createdAt = createdAt,
    comment = comment,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** Преобразует корневой продукт, сохраняя полный бизнес-payload и sync metadata. */
internal fun Product.toMirrorRow() = ProductMirrorRow(
    syncId = syncId,
    type = type,
    displayName = displayName,
    secondName = secondName,
    createdAt = createdAt,
    comment = comment,
    priceForSale = priceForSale,
    shelfLifeDays = shelfLifeDays,
    recNds = recNds,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** Преобразует транзакцию; дочерние покупки, продажи и движения маппятся отдельно. */
internal fun Transact.toMirrorRow() = TransactionMirrorRow(
    syncId = syncId,
    transactionType = transactionType,
    createdAt = createdAt,
    comment = comment,
    isCompleted = isCompleted,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)

/** Преобразует корневой эксперимент без его записей и напоминаний. */
internal fun Experiment.toMirrorRow() = ExperimentMirrorRow(
    syncId = syncId,
    title = title,
    ideaDescription = ideaDescription,
    isArchived = isArchived,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
)
