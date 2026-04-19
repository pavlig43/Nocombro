package ru.pavlig43.thermallabel.api.model

import kotlinx.datetime.LocalDate

/**
 * UI state диалога генерации термоэтикетки.
 *
 * Хранит только пользовательский выбор и флаги жизненного цикла диалога.
 * Данные, которые подгружаются из спецификации продукта, живут отдельно в [ThermalLabelPrefill].
 */
data class ThermalLabelDialogUi(
    val selectedSize: ThermalLabelSize = ThermalLabelSize.SIZE_100_150,
    val date: LocalDate,
    val massText: String = "3",
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
)

/**
 * Данные, которые автоматически подставляются в этикетку из карточки продукта и его спецификации.
 */
data class ThermalLabelPrefill(
    val productName: String,
    val composition: String,
    val dosage: String,
    val storageText: String,
)

/**
 * Полный набор данных для генерации итогового PPTX-файла этикетки.
 */
data class ThermalLabelGenerationRequest(
    val size: ThermalLabelSize,
    val productName: String,
    val composition: String,
    val dosage: String,
    val storageText: String,
    val date: LocalDate,
    val massText: String,
)
