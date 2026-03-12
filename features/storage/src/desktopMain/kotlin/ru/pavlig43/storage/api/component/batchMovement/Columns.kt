@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.storage.api.component.batchMovement

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.readDateTimeColumn
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.tableColumns

internal enum class BatchMovementField {
    DATETIME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}

internal fun createBatchMovementColumns(
): ImmutableList<ColumnSpec<BatchMovementTableUi, BatchMovementField, Unit>> {
    val columns =
        tableColumns<BatchMovementTableUi, BatchMovementField, Unit> {

            readDateTimeColumn(
                headerText = "Дата/время",
                column = BatchMovementField.DATETIME,
                valueOf = { it.movementDate },
                isSortable = false
            )

            readDecimalColumn(
                headerText = "Старт",
                column = BatchMovementField.BALANCE_BEFORE,
                valueOf = { it.balanceBeforeStart },
                isSortable = false
            )

            readDecimalColumn(
                headerText = "Приход",
                column = BatchMovementField.INCOMING,
                valueOf = { it.incoming },
                isSortable = false
            )

            readDecimalColumn(
                headerText = "Расход",
                column = BatchMovementField.OUTGOING,
                valueOf = { it.outgoing },
                isSortable = false
            )

            readDecimalColumn(
                headerText = "Остаток",
                column = BatchMovementField.BALANCE_END,
                valueOf = { it.balanceOnEnd },
                isSortable = false
            )
        }
    return columns
}

