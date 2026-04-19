package ru.pavlig43.thermallabel.api.data

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.thermallabel.api.model.ThermalLabelGenerationRequest
import ru.pavlig43.thermallabel.api.model.ThermalLabelPrefill
import ru.pavlig43.thermallabel.internal.ThermalLabelPptxGenerator

/**
 * Координирует подготовку данных для термоэтикетки.
 *
 * Сервис читает спецификацию продукта из базы, собирает prefill для диалога
 * и делегирует фактическую генерацию PPTX в [ThermalLabelPptxGenerator].
 */
class ThermalLabelTemplateService(
    db: NocombroDatabase,
) {
    private val specificationDao = db.productSpecificationDao
    private val generator = ThermalLabelPptxGenerator()

    suspend fun loadPrefill(
        productId: Int,
        productName: String,
    ): Result<ThermalLabelPrefill> {
        return runCatching {
            val specification = specificationDao.getByProductId(productId)
                ?: ProductSpecification(productId = productId)

            ThermalLabelPrefill(
                productName = productName,
                composition = specification.description.trim(),
                dosage = specification.dosage.trim(),
                storageText = buildStorageText(specification),
            )
        }
    }

    suspend fun generateLabel(
        request: ThermalLabelGenerationRequest,
    ): Result<String> {
        return runCatching {
            generator.generate(request)
        }
    }
}

/**
 * Собирает текст блока хранения и срока годности из спецификации продукта.
 *
 * Если оба поля пустые, блок целиком не выводится.
 */
private fun buildStorageText(
    specification: ProductSpecification,
): String {
    val shelfLife = specification.shelfLifeText.trim()
    val storageConditions = specification.storageConditions.trim()

    if (shelfLife.isBlank() && storageConditions.isBlank()) {
        return ""
    }

    return buildList {
        add("Хранение и срок годности:")
        if (shelfLife.isNotBlank()) {
            add(shelfLife)
        }
        if (storageConditions.isNotBlank()) {
            add(storageConditions)
        }
    }.joinToString("\n")
}
