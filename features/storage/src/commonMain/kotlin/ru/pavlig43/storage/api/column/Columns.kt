package ru.pavlig43.storage.api.column

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.immutable.internal.column.DecimalFormat
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.arrow_upward
import ua.wwind.table.ColumnSpec
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

        readTextColumn(
            column = StorageProductField.NAME,
            valueOf = { it.name },
            headerText = "Имя",
            filterType = TableFilterType.TextTableFilter()
        )
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