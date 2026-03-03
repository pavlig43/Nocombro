@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.batchMovement

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.DecimalFormat
import ru.pavlig43.immutable.internal.column.readDateTimeColumn
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class BatchMovementField {
    DATETIME,
    BATCH_NAME,
    PRODUCT_NAME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}

internal fun createBatchMovementColumn(
): ImmutableList<ColumnSpec<BatchMovementTableUi, BatchMovementField, TableData<BatchMovementTableUi>>> {
    val columns =
        tableColumns<BatchMovementTableUi, BatchMovementField, TableData<BatchMovementTableUi>> {

            readDateTimeColumn(
                headerText = "Дата/время",
                column = BatchMovementField.DATETIME,
                valueOf = { it.movementDate }
            )

            readTextColumn(
                headerText = "Партия",
                column = BatchMovementField.BATCH_NAME,
                valueOf = { it.batchName },
                filterType = TableFilterType.TextTableFilter()
            )

            readTextColumn(
                headerText = "Продукт",
                column = BatchMovementField.PRODUCT_NAME,
                valueOf = { it.productName },
                filterType = TableFilterType.TextTableFilter()
            )

            readDecimalColumn(
                headerText = "До начала",
                column = BatchMovementField.BALANCE_BEFORE,
                valueOf = { it.balanceBeforeStart },
                decimalFormat = DecimalFormat.Decimal3(),
                filterType = TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.IntDelegate)
            )

            readDecimalColumn(
                headerText = "Приход",
                column = BatchMovementField.INCOMING,
                valueOf = { it.incoming },
                decimalFormat = DecimalFormat.Decimal3(),
                filterType = TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.IntDelegate)
            )

            readDecimalColumn(
                headerText = "Расход",
                column = BatchMovementField.OUTGOING,
                valueOf = { it.outgoing },
                decimalFormat = DecimalFormat.Decimal3(),
                filterType = TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.IntDelegate)
            )

            readDecimalColumn(
                headerText = "В конце",
                column = BatchMovementField.BALANCE_END,
                valueOf = { it.balanceOnEnd },
                decimalFormat = DecimalFormat.Decimal3(),
                filterType = TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.IntDelegate)
            )
        }
    return columns
}
