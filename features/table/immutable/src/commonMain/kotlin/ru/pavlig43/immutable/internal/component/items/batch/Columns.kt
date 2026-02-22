@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.batch

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.DecimalFormat
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readDateColumn
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class BatchField {
    SELECTION,
    ID,
    BATCH_ID,
    VENDOR_NAME,
    COUNT,
    DATE_BORN
}

internal fun createBatchColumn(
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<BatchTableUi, BatchField, TableData<BatchTableUi>>> {
    val columns =
        tableColumns<BatchTableUi, BatchField, TableData<BatchTableUi>> {

            idWithSelection(
                selectionKey = BatchField.SELECTION,
                idKey = BatchField.ID,
                onEvent = onEvent
            )

            readTextColumn(
                headerText = "Поставщик",
                column = BatchField.VENDOR_NAME,
                valueOf = { it.vendorName },
                filterType = TableFilterType.TextTableFilter()
            )
            readDecimalColumn(
                headerText = "Остаток",
                column = BatchField.COUNT,
                valueOf = { it.balance },
                decimalFormat = DecimalFormat.Decimal3(),
                filterType = TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.IntDelegate)
            )

            readDateColumn(
                headerText = "Дата производства",
                column = BatchField.DATE_BORN,
                valueOf = { it.dateBorn },
                filterType = TableFilterType.DateTableFilter()
            )
        }
    return columns
}
