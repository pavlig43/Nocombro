@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingridients

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.column.readTextColumn
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

enum class IngredientField {
    SELECTION,
    COMPOSE_ID,
    PRODUCT_NAME,
    COUNT,
    VENDOR_NAME,
    BATCH_ID
}

@Suppress("LongMethod")
internal fun createIngredientColumns(
    onOpenProductDialog: (Int) -> Unit,
    onEvent: (MutableUiEvent) -> Unit
): ImmutableList<ColumnSpec<IngredientUi, IngredientField, TableData<IngredientUi>>> {
    val columns =
        editableTableColumns<IngredientUi, IngredientField, TableData<IngredientUi>> {

            idWithSelection(
                selectionKey = IngredientField.SELECTION,
                idKey = IngredientField.COMPOSE_ID,
                onEvent = onEvent
            )

            textWithSearchIconColumn(
                headerText = "Продукт",
                column = IngredientField.PRODUCT_NAME,
                valueOf = { it.productName },
                onOpenDialog = { onOpenProductDialog(it.composeId) },
                filterType = TableFilterType.TextTableFilter()
            )

            decimalColumn(
                key = IngredientField.COUNT,
                getValue = { it.count },
                headerText = "Количество",
                decimalFormat = DecimalFormat.Decimal3(),
                updateItem = { item, count -> onEvent(MutableUiEvent.UpdateItem(item.copy(count = count))) },
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.count } }
            )

            readTextColumn(
                headerText = "Поставщик",
                column = IngredientField.VENDOR_NAME,
                valueOf = { it.vendorName },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Партия",
                column = IngredientField.BATCH_ID,
                valueOf = { it.batchId.toString() },
                filterType = TableFilterType.TextTableFilter()
            )
        }
    return columns
}
