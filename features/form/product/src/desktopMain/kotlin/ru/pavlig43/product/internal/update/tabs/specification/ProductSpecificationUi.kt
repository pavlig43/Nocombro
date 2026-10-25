package ru.pavlig43.product.internal.update.tabs.specification

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt

internal data class ProductSpecificationUi(
    val id: Int,
    val productId: Int,
    val dosageState: TextFieldState = TextFieldState(),
    val compositionState: TextFieldState = TextFieldState(),
    val shelfLifeTextState: TextFieldState = TextFieldState(),
    val storageConditionsState: TextFieldState = TextFieldState(),
    val appearanceState: TextFieldState = TextFieldState(),
    val colorState: TextFieldState = TextFieldState(),
    val smellState: TextFieldState = TextFieldState(),
    val tasteState: TextFieldState = TextFieldState(),
    val physicalChemicalIndicatorsState: TextFieldState = TextFieldState(),
    val microbiologicalIndicatorsState: TextFieldState = TextFieldState(),
    val toxicElementsState: TextFieldState = TextFieldState(),
    val allergensState: TextFieldState = TextFieldState(),
    val gmoInfoState: TextFieldState = TextFieldState(),
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
) : SingleLineInputState {
    val dosage: String get() = dosageState.text.toString()
    val composition: String get() = compositionState.text.toString()
    val shelfLifeText: String get() = shelfLifeTextState.text.toString()
    val storageConditions: String get() = storageConditionsState.text.toString()
    val appearance: String get() = appearanceState.text.toString()
    val color: String get() = colorState.text.toString()
    val smell: String get() = smellState.text.toString()
    val taste: String get() = tasteState.text.toString()
    val physicalChemicalIndicators: String get() = physicalChemicalIndicatorsState.text.toString()
    val microbiologicalIndicators: String get() = microbiologicalIndicatorsState.text.toString()
    val toxicElements: String get() = toxicElementsState.text.toString()
    val allergens: String get() = allergensState.text.toString()
    val gmoInfo: String get() = gmoInfoState.text.toString()
    override val textFields get() = listOf(
        dosageState,
        compositionState,
        shelfLifeTextState,
        storageConditionsState,
        appearanceState,
        colorState,
        smellState,
        tasteState,
        physicalChemicalIndicatorsState,
        microbiologicalIndicatorsState,
        toxicElementsState,
        allergensState,
        gmoInfoState,
    )
}

internal fun ProductSpecificationUi.toDto(): ProductSpecification {
    return ProductSpecification(
        productId = productId,
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
        allergens = editorTextToAllergensJson(allergens),
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
        dosageState = TextFieldState(dosage),
        compositionState = TextFieldState(composition),
        shelfLifeTextState = TextFieldState(shelfLifeText),
        storageConditionsState = TextFieldState(storageConditions),
        appearanceState = TextFieldState(appearance),
        colorState = TextFieldState(color),
        smellState = TextFieldState(smell),
        tasteState = TextFieldState(taste),
        physicalChemicalIndicatorsState = TextFieldState(physicalChemicalIndicators),
        microbiologicalIndicatorsState = TextFieldState(microbiologicalIndicators),
        toxicElementsState = TextFieldState(toxicElements),
        allergensState = TextFieldState(allergensToEditorText(allergens)),
        gmoInfoState = TextFieldState(gmoInfo),
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
