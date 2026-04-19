package ru.pavlig43.thermallabel.api.model

import kotlinx.datetime.LocalDate

data class ThermalLabelDialogUi(
    val selectedSize: ThermalLabelSize = ThermalLabelSize.entries.first(),
    val date: LocalDate,
    val massText: String = "3",
    val isLoading: Boolean = true,
    val isGenerating: Boolean = false,
)

data class ThermalLabelPrefill(
    val productName: String,
    val composition: String,
    val dosage: String,
    val storageText: String,
)

data class ThermalLabelGenerationRequest(
    val size: ThermalLabelSize,
    val productName: String,
    val composition: String,
    val dosage: String,
    val storageText: String,
    val date: LocalDate,
    val massText: String,
)
