package ru.pavlig43.database.data.sync.mirror

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.experiment.ExperimentEntry
import ru.pavlig43.database.data.experiment.ExperimentReminder
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.product.SafetyStock

/**
 * Преобразует декларацию и заменяет локальный `vendorId` на `vendorSyncId`.
 *
 * Отсутствующий vendor считается нарушением локального FK-инварианта и приводит
 * к ошибке DAO/mapper вместо создания неполной remote row.
 */
internal suspend fun Declaration.toMirrorRow(db: NocombroDatabase): DeclarationMirrorRow {
    val vendor = db.vendorDao.getVendor(vendorId)
    return DeclarationMirrorRow(
        syncId = syncId,
        displayName = displayName,
        createdAt = createdAt,
        vendorSyncId = vendor.syncId,
        vendorName = vendorName,
        bornDate = bornDate,
        bestBefore = bestBefore,
        observeFromNotification = observeFromNotification,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/** Преобразует спецификацию, разрешая ее локальный product id в стабильный sync id. */
internal suspend fun ProductSpecification.toMirrorRow(db: NocombroDatabase): ProductSpecificationMirrorRow {
    val product = db.productDao.getProduct(productId)
    return ProductSpecificationMirrorRow(
        syncId = syncId,
        productSyncId = product.syncId,
        dosage = dosage,
        composition = composition,
        shelfLifeText = shelfLifeText,
        storageConditions = storageConditions,
        appearance = appearance,
        color = color,
        smell = smell,
        taste = taste,
        physicalChemicalIndicators = physicalChemicalIndicators,
        microbiologicalIndicators = microbiologicalIndicators,
        toxicElements = toxicElements,
        allergens = allergens,
        gmoInfo = gmoInfo,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/** Преобразует страховой запас со ссылкой на продукт через `productSyncId`. */
internal suspend fun SafetyStock.toMirrorRow(db: NocombroDatabase): SafetyStockMirrorRow {
    val product = db.productDao.getProduct(productId)
    return SafetyStockMirrorRow(
        syncId = syncId,
        productSyncId = product.syncId,
        reorderPoint = reorderPoint,
        orderQuantity = orderQuantity,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/**
 * Преобразует запись эксперимента и требует существующего родительского experiment.
 */
internal suspend fun ExperimentEntry.toMirrorRow(db: NocombroDatabase): ExperimentEntryMirrorRow {
    val experiment = db.experimentDao.getExperiment(experimentId)
        ?: error("Missing experiment dependency for id=$experimentId")
    return ExperimentEntryMirrorRow(
        syncId = syncId,
        experimentSyncId = experiment.syncId,
        entryDate = entryDate,
        content = content,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

/**
 * Преобразует напоминание эксперимента и переносит родительскую связь через sync id.
 */
internal suspend fun ExperimentReminder.toMirrorRow(db: NocombroDatabase): ExperimentReminderMirrorRow {
    val experiment = db.experimentDao.getExperiment(experimentId)
        ?: error("Missing experiment dependency for id=$experimentId")
    return ExperimentReminderMirrorRow(
        syncId = syncId,
        experimentSyncId = experiment.syncId,
        text = text,
        reminderDateTime = reminderDateTime,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
