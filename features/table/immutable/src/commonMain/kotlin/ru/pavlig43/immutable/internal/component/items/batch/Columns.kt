@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.batch

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.idWithSelection
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
                headerText = "ID партии",
                column = BatchField.BATCH_ID,
                valueOf = { it.batchId.toString() },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Поставщик",
                column = BatchField.VENDOR_NAME,
                valueOf = { it.vendorName },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Остаток",
                column = BatchField.COUNT,
                valueOf = { it.count.toString() },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Дата создания",
                column = BatchField.DATE_BORN,
                valueOf = { it.dateBorn.toString() },
                filterType = TableFilterType.TextTableFilter()
            )
        }
    return columns
}
