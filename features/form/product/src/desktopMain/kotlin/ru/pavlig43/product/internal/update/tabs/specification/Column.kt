@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.product.internal.update.tabs.specification

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.writeTextColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

internal enum class ProductSpecificationField {
    DESCRIPTION,
    DOSAGE,
    COMPOSITION,
    SHELF_LIFE_TEXT,
    STORAGE_CONDITIONS,
    APPEARANCE,
    COLOR,
    SMELL,
    TASTE,
    PHYSICAL_CHEMICAL_INDICATORS,
    MICROBIOLOGICAL_INDICATORS,
    TOXIC_ELEMENTS,
    ALLERGENS,
    GMO_INFO,
}

@Suppress("LongMethod")
internal fun createProductSpecificationColumns(
    onChangeItem: ((ProductSpecificationUi) -> ProductSpecificationUi) -> Unit
): ImmutableList<ColumnSpec<ProductSpecificationUi, ProductSpecificationField, Unit>> {
    return editableTableColumns {
        writeTextColumn(
            headerText = "Описание",
            column = ProductSpecificationField.DESCRIPTION,
            valueOf = { it.description },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(description = newValue) } }
        )
        writeTextColumn(
            headerText = "Дозировка",
            column = ProductSpecificationField.DOSAGE,
            valueOf = { it.dosage },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(dosage = newValue) } }
        )
        writeTextColumn(
            headerText = "Состав",
            column = ProductSpecificationField.COMPOSITION,
            valueOf = { it.composition },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(composition = newValue) } }
        )
        writeTextColumn(
            headerText = "Срок годности",
            column = ProductSpecificationField.SHELF_LIFE_TEXT,
            valueOf = { it.shelfLifeText },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(shelfLifeText = newValue) } }
        )
        writeTextColumn(
            headerText = "Условия хранения",
            column = ProductSpecificationField.STORAGE_CONDITIONS,
            valueOf = { it.storageConditions },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(storageConditions = newValue) } }
        )
        writeTextColumn(
            headerText = "Органолептика: внешний вид",
            column = ProductSpecificationField.APPEARANCE,
            valueOf = { it.appearance },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(appearance = newValue) } }
        )
        writeTextColumn(
            headerText = "Органолептика: цвет",
            column = ProductSpecificationField.COLOR,
            valueOf = { it.color },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(color = newValue) } }
        )
        writeTextColumn(
            headerText = "Органолептика: запах",
            column = ProductSpecificationField.SMELL,
            valueOf = { it.smell },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(smell = newValue) } }
        )
        writeTextColumn(
            headerText = "Органолептика: вкус",
            column = ProductSpecificationField.TASTE,
            valueOf = { it.taste },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(taste = newValue) } }
        )
        writeTextColumn(
            headerText = "Физико-химические показатели",
            column = ProductSpecificationField.PHYSICAL_CHEMICAL_INDICATORS,
            valueOf = { it.physicalChemicalIndicators },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue ->
                onChangeItem { it.copy(physicalChemicalIndicators = newValue) }
            }
        )
        writeTextColumn(
            headerText = "Микробиологические показатели",
            column = ProductSpecificationField.MICROBIOLOGICAL_INDICATORS,
            valueOf = { it.microbiologicalIndicators },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue ->
                onChangeItem { it.copy(microbiologicalIndicators = newValue) }
            }
        )
        writeTextColumn(
            headerText = "Токсичные элементы",
            column = ProductSpecificationField.TOXIC_ELEMENTS,
            valueOf = { it.toxicElements },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(toxicElements = newValue) } }
        )
        writeTextColumn(
            headerText = "Аллергены",
            column = ProductSpecificationField.ALLERGENS,
            valueOf = { it.allergens },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(allergens = newValue) } }
        )
        writeTextColumn(
            headerText = "Информация о ГМО",
            column = ProductSpecificationField.GMO_INFO,
            valueOf = { it.gmoInfo },
            isSortable = false,
            singleLine = false,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(gmoInfo = newValue) } }
        )
    }
}
