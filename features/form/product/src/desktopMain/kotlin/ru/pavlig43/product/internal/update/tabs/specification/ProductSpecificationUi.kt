package ru.pavlig43.product.internal.update.tabs.specification

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

internal data class ProductSpecificationUi(
    val id: Int,
    val productId: Int,
    val description: String,
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
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
) : ISingleLineTableUi

internal fun ProductSpecificationUi.toDto(): ProductSpecification {
    return ProductSpecification(
        productId = productId,
        description = description,
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
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

internal fun ProductSpecification.toUi(): ProductSpecificationUi {
    return ProductSpecificationUi(
        id = id,
        productId = productId,
        description = description,
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
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
