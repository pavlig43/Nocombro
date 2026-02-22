@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.transaction.internal.update.tabs.component.sale

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.idWithSelection
import ru.pavlig43.mutable.api.column.readDecimalColumnWithFooter
import ru.pavlig43.mutable.api.column.readDateColumn
import ru.pavlig43.mutable.api.column.readTextColumn
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

enum class SaleField {
    SELECTION,
    COMPOSE_ID,
    COUNT,
    SUM,
    BATCH_ID,
    PRODUCT_NAME,
    VENDOR_NAME,
    DATE_BORN,
    CLIENT_NAME,
    PRICE,
    COMMENT
}


@Suppress("LongMethod")
internal fun createSaleColumn(
    onOpenProductDialog: (Int) -> Unit,
    onOpenBatchDialog: (Int, Int) -> Unit,
    onOpenClientDialog: (Int) -> Unit,
    onEvent: (MutableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<SaleUi, SaleField, TableData<SaleUi>>> {
    val columns =
        editableTableColumns<SaleUi, SaleField, TableData<SaleUi>> {

            idWithSelection(
                selectionKey = SaleField.SELECTION,
                idKey = SaleField.COMPOSE_ID,
                onEvent = onEvent
            )

            textWithSearchIconColumn(
                headerText = "Продукт",
                column = SaleField.PRODUCT_NAME,
                valueOf = { it.productName },
                onOpenDialog = { onOpenProductDialog(it.composeId) },
                filterType = TableFilterType.TextTableFilter()
            )

            decimalColumn(
                key = SaleField.COUNT,
                getValue = { it.count },
                headerText = "Количество",
                decimalFormat = DecimalFormat.Decimal3(),
                updateItem = { item, count -> onEvent(MutableUiEvent.UpdateItem(item.copy(count = count))) },
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.count } }
            )

            decimalColumn(
                key = SaleField.PRICE,
                getValue = { it.price },
                headerText = "Цена",
                decimalFormat = DecimalFormat.Decimal2(),
                updateItem = { item, price -> onEvent(MutableUiEvent.UpdateItem(item.copy(price = price))) }
            )

            readDecimalColumnWithFooter(
                key = SaleField.SUM,
                getValue = { it.sum },
                headerText = "Сумма",
                decimalFormat = DecimalFormat.Decimal2(),
                footerValue = { tableData -> tableData.displayedItems.sumOf { it.sum } }
            )

            textWithSearchIconColumn(
                headerText = "Партия",
                column = SaleField.BATCH_ID,
                valueOf = { it.batchId.toString() },
                onOpenDialog = { item -> onOpenBatchDialog(item.composeId, item.productId) },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Поставщик",
                column = SaleField.VENDOR_NAME,
                valueOf = { it.vendorName },
                filterType = TableFilterType.TextTableFilter()
            )

            readDateColumn(
                headerText = "Дата производства",
                column = SaleField.DATE_BORN,
                valueOf = { it.dateBorn },
                filterType = TableFilterType.DateTableFilter()
            )

            textWithSearchIconColumn(
                headerText = "Клиент",
                column = SaleField.CLIENT_NAME,
                valueOf = { it.clientName },
                onOpenDialog = { onOpenClientDialog(it.composeId) },
                filterType = TableFilterType.TextTableFilter()
            )

            writeTextColumn(
                headerText = "Комментарий",
                column = SaleField.COMMENT,
                valueOf = { it.comment },
                onChangeItem = { item, newValue -> onEvent(MutableUiEvent.UpdateItem(item.copy(comment = newValue))) },
                filterType = TableFilterType.TextTableFilter()
            )
        }
    return columns
}


