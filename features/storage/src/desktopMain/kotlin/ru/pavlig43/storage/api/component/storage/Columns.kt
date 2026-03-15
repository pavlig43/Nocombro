@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.storage.api.component.storage

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
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

        column(StorageProductField.EXPAND, valueOf = { it.isExpanded }) {
            title { "" }
            autoWidth()
            cell { item, _ ->
                ExpandedCell(item, onToggleExpand)
            }
        }
        nameColumn()

        decimalColumn(
            column = StorageProductField.BALANCE_BEFORE,
            title = "Старт",
            valueOf = {it.balanceBeforeStart}
        )
        decimalColumn(
            column = StorageProductField.INCOMING,
            title = "Приход",
            valueOf = {it.incoming}
        )
        decimalColumn(
            column = StorageProductField.OUTGOING,
            title = "Расход",
            valueOf = {it.outgoing}
        )
        decimalColumn(
            column = StorageProductField.BALANCE_END,
            title = "Остаток",
            valueOf = {it.balanceOnEnd}
        )


    }

@Composable
private fun ExpandedCell(
    item: StorageProductUi,
    onToggleExpand: (productId: Int) -> Unit
) {
    if (item.isProduct && !item.isExpanded) {
        ToolTipIconButton(
            tooltipText = "Развернуть",
            onClick = { onToggleExpand(item.itemId) },
            icon = Res.drawable.arrow_downward,
        )
    }
    if (item.isProduct && item.isExpanded) {
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
    column(key = StorageProductField.NAME, valueOf = { it.itemName }) {
        title { "Имя" }
        autoWidth()
        cell { item, _ ->
            val padding = if (item.isProduct) 4.dp else 16.dp
            Text(item.itemName, modifier = Modifier.padding(start = padding, end = 8.dp))
        }
        filter(TableFilterType.TextTableFilter())
    }

}
private fun ReadonlyTableColumnsBuilder<StorageProductUi, StorageProductField, StorageTableData>.decimalColumn(
    column:StorageProductField,
    title:String,
    valueOf:(StorageProductUi)-> Int,
) {

    column(key = column, valueOf = valueOf) {
        title { title }
        autoWidth()
        cell { item, _ ->
            val padding = if (item.isProduct) 8.dp else 20.dp
            Text(
                text = DecimalData3(valueOf(item)).toStartDoubleFormat(),
                modifier = Modifier.padding(start = padding, end = 12.dp)
            )
        }
    }

}
