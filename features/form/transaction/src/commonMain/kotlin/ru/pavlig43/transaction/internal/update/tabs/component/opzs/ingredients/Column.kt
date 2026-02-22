@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import kotlinx.collections.immutable.ImmutableList
import kotlinx.datetime.format
import ru.pavlig43.core.dateFormat
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
    BATCH_NAME
}

@Suppress("LongMethod")
internal fun createIngredientColumns(
    onOpenProductDialog: (Int) -> Unit,
    onOpenBatchDialog: (Int) -> Unit,
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
                getValue = { it.balance },
                headerText = "Количество",
                decimalFormat = DecimalFormat.Decimal3(),
                updateItem = { item, count -> onEvent(MutableUiEvent.UpdateItem(item.copy(balance = count))) },
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.balance } }
            )
            textWithSearchIconColumn(
                headerText = "Партия",
                column = IngredientField.BATCH_NAME,
                valueOf = {
                    "(${it.batchId}) ${it.dateBorn.format(dateFormat)}"
                },
                onOpenDialog = {
                    if (it.productId != 0) {
                        onOpenBatchDialog(it.composeId)
                    }
                },
                filterType = TableFilterType.TextTableFilter()
            )
            readTextColumn(
                headerText = "Поставщик",
                column = IngredientField.VENDOR_NAME,
                valueOf = { it.vendorName },
                filterType = TableFilterType.TextTableFilter()
            )

        }
    return columns
}
