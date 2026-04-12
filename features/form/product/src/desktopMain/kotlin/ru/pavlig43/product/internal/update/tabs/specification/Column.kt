@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.product.internal.update.tabs.specification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.writeTextColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableCellTextField
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
    onChangeItem: ((ProductSpecificationUi) -> ProductSpecificationUi) -> Unit,
    onGenerateComposition: () -> Unit,
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
        column(
            key = ProductSpecificationField.COMPOSITION,
            valueOf = { it.composition },
        ) {
            width(min = 260.dp)
            autoWidth(360.dp)
            header("Состав")
            cell { item, _ ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(item.composition)
                    TextButton(
                        onClick = onGenerateComposition,
                    ) {
                        Text("Сгенерировать")
                    }
                }
            }
            editCell { item, _, _ ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    TextButton(
                        onClick = onGenerateComposition,
                    ) {
                        Text("Сгенерировать")
                    }
                    TableCellTextField(
                        value = item.composition,
                        onValueChange = { newValue -> onChangeItem { it.copy(composition = newValue) } },
                        singleLine = false,
                    )
                }
            }
            resizable(true)
        }
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
            headerText = "Внешний вид",
            column = ProductSpecificationField.APPEARANCE,
            valueOf = { it.appearance },
            isSortable = false,
            singleLine = false,
            minWidth = 180.dp,
            autoMaxWidth = 220.dp,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(appearance = newValue) } }
        )
        writeTextColumn(
            headerText = "Цвет",
            column = ProductSpecificationField.COLOR,
            valueOf = { it.color },
            isSortable = false,
            singleLine = false,
            minWidth = 120.dp,
            autoMaxWidth = 150.dp,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(color = newValue) } }
        )
        writeTextColumn(
            headerText = "Запах",
            column = ProductSpecificationField.SMELL,
            valueOf = { it.smell },
            isSortable = false,
            singleLine = false,
            minWidth = 120.dp,
            autoMaxWidth = 150.dp,
            onChangeItem = { _, newValue -> onChangeItem { it.copy(smell = newValue) } }
        )
        writeTextColumn(
            headerText = "Вкус",
            column = ProductSpecificationField.TASTE,
            valueOf = { it.taste },
            isSortable = false,
            singleLine = false,
            minWidth = 120.dp,
            autoMaxWidth = 150.dp,
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
            headerText = "Аллергены (формат: аллерген; в продукте да/нет; на производстве да/нет)",
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
