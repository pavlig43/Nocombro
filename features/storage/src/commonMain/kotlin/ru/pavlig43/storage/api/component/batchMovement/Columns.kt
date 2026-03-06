@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.storage.api.component.batchMovement

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.DecimalFormat
import ru.pavlig43.immutable.internal.column.readDateTimeColumn
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.tableColumns

internal enum class BatchMovementField {
    DATETIME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}

internal fun createBatchMovementColumns(
): ImmutableList<ColumnSpec<BatchMovementTableUi, BatchMovementField, BatchMovementTableData>> {
    val columns =
        tableColumns<BatchMovementTableUi, BatchMovementField, BatchMovementTableData> {

            readDateTimeColumn(
                headerText = "Дата/время",
                column = BatchMovementField.DATETIME,
                valueOf = { it.movementDate }
            )

            readDecimalColumn(
                headerText = "До начала",
                column = BatchMovementField.BALANCE_BEFORE,
                valueOf = { it.balanceBeforeStart },
                decimalFormat = DecimalFormat.Decimal3()
            )

            readDecimalColumn(
                headerText = "Приход",
                column = BatchMovementField.INCOMING,
                valueOf = { it.incoming },
                decimalFormat = DecimalFormat.Decimal3()
            )

            readDecimalColumn(
                headerText = "Расход",
                column = BatchMovementField.OUTGOING,
                valueOf = { it.outgoing },
                decimalFormat = DecimalFormat.Decimal3()
            )

            readDecimalColumn(
                headerText = "В конце",
                column = BatchMovementField.BALANCE_END,
                valueOf = { it.balanceOnEnd },
                decimalFormat = DecimalFormat.Decimal3()
            )
        }
    return columns
}

internal data class BatchMovementTableData(
    val items: List<BatchMovementTableUi> = emptyList()
)
