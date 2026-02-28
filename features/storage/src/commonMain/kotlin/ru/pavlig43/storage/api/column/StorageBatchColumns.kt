package ru.pavlig43.storage.api.column

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.immutable.internal.column.DecimalFormat
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.storage.internal.model.StorageBatchUi
import ru.pavlig43.storage.internal.model.StorageProductUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.tableColumns

enum class StorageBatchColumn {
    EMPTY,
    BATCH_NAME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}

fun createStorageBatchColumns(): ImmutableList<ColumnSpec<StorageBatchUi, StorageBatchColumn, StorageProductUi>> =
    tableColumns {

        readTextColumn(
            column = StorageBatchColumn.EMPTY,
            headerText = "",
            valueOf = { ""},
        )
        readTextColumn(
            column = StorageBatchColumn.BATCH_NAME,
            valueOf = { it.batchName },
            headerText = ""
        )

        readDecimalColumn(
            column = StorageBatchColumn.BALANCE_BEFORE,
            valueOf = { it.balanceBeforeStart },
            headerText = "Старт",
            decimalFormat = DecimalFormat.Decimal3(),
        )

        readDecimalColumn(
            column = StorageBatchColumn.INCOMING,
            valueOf = { it.incoming },
            headerText = "Приход",
            decimalFormat = DecimalFormat.Decimal3(),
        )

        readDecimalColumn(
            column = StorageBatchColumn.OUTGOING,
            valueOf = { it.outgoing },
            headerText = "Расход",
            decimalFormat = DecimalFormat.Decimal3(),
        )

        readDecimalColumn(
            column = StorageBatchColumn.BALANCE_END,
            valueOf = { it.balanceOnEnd },
            headerText = "Остаток",
            decimalFormat = DecimalFormat.Decimal3(),
        )
    }


