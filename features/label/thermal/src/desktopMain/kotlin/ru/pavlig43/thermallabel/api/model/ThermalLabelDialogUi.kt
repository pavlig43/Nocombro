package ru.pavlig43.thermallabel.api.model

import androidx.compose.foundation.text.input.TextFieldState
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
    val massState: TextFieldState = TextFieldState("3"),
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
) {
    val massText: String get() = massState.text.toString()
}

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
