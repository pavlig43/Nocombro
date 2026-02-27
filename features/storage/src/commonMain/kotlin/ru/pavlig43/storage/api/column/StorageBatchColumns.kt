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
import ru.pavlig43.storage.internal.model.StorageBatchUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.editableTableColumns

fun createStorageBatchColumns(): ImmutableList<ColumnSpec<StorageBatchUi, StorageBatchColumn, ru.pavlig43.storage.internal.model.StorageProductUi>> =
    editableTableColumns {
        readTextColumn(
            key = StorageBatchColumn.BATCH_NAME,
            getValue = { it.batchName },
            headerText = "Партия"
        )

        readDecimalColumn(
            key = StorageBatchColumn.BALANCE_BEFORE,
            getValue = { it.balanceBeforeStart },
            headerText = "Остаток на начало (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )

        readDecimalColumn(
            key = StorageBatchColumn.INCOMING,
            getValue = { it.incoming },
            headerText = "Приход (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )

        readDecimalColumn(
            key = StorageBatchColumn.OUTGOING,
            getValue = { it.outgoing },
            headerText = "Расход (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )

        readDecimalColumn(
            key = StorageBatchColumn.BALANCE_END,
            getValue = { it.balanceOnEnd },
            headerText = "Остаток на конец (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            isSortable = false
        )
    }

private fun <T : Any, C, E> EditableTableColumnsBuilder<T, C, E>.readTextColumn(
    key: C,
    getValue: (T) -> String,
    headerText: String
) {
    column(key, valueOf = { getValue(it) }) {
        autoWidth(300.dp)
        header(headerText)
        cell { item, _ ->
            Text(
                text = getValue(item),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}
