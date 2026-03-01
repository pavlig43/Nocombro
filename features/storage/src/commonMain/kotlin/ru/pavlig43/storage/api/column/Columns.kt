package ru.pavlig43.storage.api.column

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.immutable.internal.column.DecimalFormat
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.arrow_upward
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

enum class StorageProductField {
    EXPAND,
    NAME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}

internal fun createStorageColumns(
    onToggleExpand: (productId: Int) -> Unit
): ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, StorageTableData>> =
    tableColumns {

        column(StorageProductField.EXPAND, valueOf = { it.expanded }) {
            title { "" }
            autoWidth()
            cell { item, _ ->
                ExpandedCell(item, onToggleExpand)
            }
        }
        nameColumn()

        readDecimalColumn(
            headerText = "Старт",
            column = StorageProductField.BALANCE_BEFORE,
            valueOf = { it.balanceBeforeStart },
            decimalFormat = DecimalFormat.Decimal3(),
            filterType = TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate
            )
        )
        readDecimalColumn(
            headerText = "Приход",
            column = StorageProductField.INCOMING,
            valueOf = { it.incoming },
            decimalFormat = DecimalFormat.Decimal3(),
            filterType = TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate
            )
        )
        readDecimalColumn(
            headerText = "Расход",
            column = StorageProductField.OUTGOING,
            valueOf = { it.outgoing },
            decimalFormat = DecimalFormat.Decimal3(),
            filterType = TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate
            )
        )
        readDecimalColumn(
            headerText = "Остаток",
            column = StorageProductField.BALANCE_END,
            valueOf = { it.balanceOnEnd },
            decimalFormat = DecimalFormat.Decimal3(),
            filterType = TableFilterType.NumberTableFilter(
                delegate = TableFilterType.NumberTableFilter.IntDelegate
            )
        )

    }

@Composable
private fun ExpandedCell(
    item: StorageProductUi,
    onToggleExpand: (productId: Int) -> Unit
) {
    if (item.isProduct && !item.expanded) {
        ToolTipIconButton(
            tooltipText = "Развернуть",
            onClick = { onToggleExpand(item.itemId) },
            icon = Res.drawable.arrow_downward,
        )
    }
    if (item.isProduct && item.expanded) {
        ToolTipIconButton(
            tooltipText = "Свернуть",
            onClick = { onToggleExpand(item.itemId) },
            icon = Res.drawable.arrow_upward,
        )
    }
    if (!item.isProduct) {
        Text("")
    }

}

private fun ReadonlyTableColumnsBuilder<StorageProductUi, StorageProductField, StorageTableData>.nameColumn(
) {
    column(key = StorageProductField.NAME, valueOf = { it.name }) {
        title { "Имя" }
        autoWidth()
        cell { item, _ ->
            val padding = if (item.isProduct) 4.dp else 16.dp
            Text(item.name, modifier = Modifier.padding(start = padding, end = 8.dp))
        }
    }

}
//private fun ReadonlyTableColumnsBuilder<StorageProductUi, StorageProductField, StorageTableData>.decimalColumn(
//    column:StorageProductField,
//    title:String,
//    valueOf:(StorageProductUi)->Int,
//    decimalFormat:DecimalFormat,
//    filterType:TableFilterType.NumberTableFilter
//) {
//
//    column(key = column, valueOf = valueOf) {
//        title { title }
//        autoWidth()
//        cell { item, _ ->
//            Text(
//                text = valueOf(item).toStartDoubleFormat(format),
//                modifier = Modifier.padding(horizontal = 12.dp)
//            )
//        }
//    }
//
//}