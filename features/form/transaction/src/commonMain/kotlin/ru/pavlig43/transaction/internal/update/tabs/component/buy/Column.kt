@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.buy

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.column.readTextColumn
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ru.pavlig43.mutable.api.column.writeDateColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

enum class BuyField {
    SELECTION,
    COMPOSE_ID,
    COUNT,

    BATCH_ID,
    PRODUCT_NAME,
    DECLARATION_NAME,
    VENDOR_NAME,
    DATE_BORN,
    PRICE,
    COMMENT
}


@Suppress("LongMethod")
internal fun createBuyColumn(
    onOpenProductDialog: (Int) -> Unit,
    onOpenDeclarationDialog: (Int, Int) -> Unit,
    onOpenDateDialog:(Int) -> Unit,
    onEvent: (MutableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<BuyUi, BuyField, TableData<BuyUi>>> {
    val columns =
        editableTableColumns<BuyUi, BuyField, TableData<BuyUi>> {

            idWithSelection(
                selectionKey = BuyField.SELECTION,
                idKey = BuyField.COMPOSE_ID,
                onEvent = onEvent
            )

            textWithSearchIconColumn(
                headerText = "Продукт",
                column = BuyField.PRODUCT_NAME,
                valueOf = { it.productName },
                onOpenDialog = { onOpenProductDialog(it.composeId) },
                filterType = TableFilterType.TextTableFilter()
            )

            decimalColumn(
                key = BuyField.COUNT,
                getValue = { it.count },
                headerText = "Количество",
                decimalFormat = DecimalFormat.KG(),
                updateItem = { item, count -> onEvent(MutableUiEvent.UpdateItem(item.copy(count = count))) },
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.count } }
            )

            decimalColumn(
                key = BuyField.PRICE,
                getValue = { it.price },
                headerText = "Цена",
                decimalFormat = DecimalFormat.RUB(),
                updateItem = { item, price -> onEvent(MutableUiEvent.UpdateItem(item.copy(price = price))) },
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.price } }
            )
           readTextColumn(
               headerText = "Партия",
               column = BuyField.BATCH_ID,
               valueOf = { it.batchId.toString() },
               filterType = TableFilterType.TextTableFilter()
           )

            textWithSearchIconColumn(
                headerText = "Декларация",
                column = BuyField.DECLARATION_NAME,
                valueOf = { it.declarationName },
                onOpenDialog = { onOpenDeclarationDialog(it.composeId, it.productId) },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Поставщик",
                column = BuyField.VENDOR_NAME,
                valueOf = { it.vendorName },
                filterType = TableFilterType.TextTableFilter()
            )

            writeDateColumn(
                headerText = "Дата производства",
                column = BuyField.DATE_BORN,
                valueOf = { it.dateBorn },
                onOpenDateDialog = {item-> onOpenDateDialog(item.composeId)}
            )

            writeTextColumn(
                headerText = "Комментарий",
                column = BuyField.COMMENT,
                valueOf = { it.comment },
                onChangeItem = { item, newValue -> onEvent(MutableUiEvent.UpdateItem(item.copy(comment = newValue))) },
                filterType = TableFilterType.TextTableFilter()
            )
        }
    return columns
}



