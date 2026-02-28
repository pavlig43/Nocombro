package ru.pavlig43.storage.api.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.readDecimalColumn
import ru.pavlig43.mutable.api.column.readTextColumn
import ru.pavlig43.storage.internal.model.StorageBatchUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.editableTableColumns

fun createStorageBatchColumns(): ImmutableList<ColumnSpec<StorageBatchUi, StorageBatchColumn, ru.pavlig43.storage.internal.model.StorageProductUi>> =
    editableTableColumns {
        readTextColumn(
            column = StorageBatchColumn.BATCH_NAME,
            valueOf = { it.batchName },
            headerText = "Партия"
        )

        readDecimalColumn(
            key = StorageBatchColumn.BALANCE_BEFORE,
            getValue = { it.balanceBeforeStart },
            headerText = "Старт",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )

        readDecimalColumn(
            key = StorageBatchColumn.INCOMING,
            getValue = { it.incoming },
            headerText = "Приход",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )

        readDecimalColumn(
            key = StorageBatchColumn.OUTGOING,
            getValue = { it.outgoing },
            headerText = "Расход",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )

        readDecimalColumn(
            key = StorageBatchColumn.BALANCE_END,
            getValue = { it.balanceOnEnd },
            headerText = "Остаток",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )
    }


