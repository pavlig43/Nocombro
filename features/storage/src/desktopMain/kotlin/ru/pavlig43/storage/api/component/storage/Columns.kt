@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.storage.api.component.storage

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.coreui.tooltip.ToolTipIconButton
import ru.pavlig43.mutable.api.column.NameRowWithSearchIcon
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.ui.SearchHighlightedText
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
    VENDOR_NAME,
    BALANCE_BEFORE,
    INCOMING,
    OUTGOING,
    BALANCE_END
}

/**
 * Создаёт колонки таблицы склада с центрированными заголовками и значениями.
 *
 * Строки товаров используют общую ячейку с кнопкой открытия карточки, а строки
 * партий остаются информационными и не участвуют в навигации к товару.
 */
internal fun createStorageColumns(
    onToggleExpand: (productId: Int) -> Unit,
    onToggleExpandAll: () -> Unit,
    onOpenProduct: (productId: Int) -> Unit,
    searchQuery: () -> String,
): ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, StorageTableData>> =
    tableColumns {

        column(StorageProductField.EXPAND, valueOf = { it.isExpanded }) {
            header { tableData ->
                ExpandedHeader(
                    areAllProductsExpanded = tableData.areAllProductsExpanded,
                    onToggleExpandAll = onToggleExpandAll,
                )
            }
            autoWidth()
            align(Alignment.Center)
            cell { item, _ ->
                ExpandedCell(item, onToggleExpand)
            }
        }
        nameColumn(onOpenProduct, searchQuery)

        column(StorageProductField.VENDOR_NAME, valueOf = { it.vendorNames }) {
            title { "Поставщик" }
            autoWidth()
            align(Alignment.Center)
            cell { item, _ ->
                SearchHighlightedText(
                    text = item.vendorNames,
                    query = searchQuery().takeIf { item.isProduct }.orEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            filter(TableFilterType.TextTableFilter())
        }

        decimalColumn(
            column = StorageProductField.BALANCE_BEFORE,
            title = "Старт",
            valueOf = { it.balanceBeforeStart },
            searchQuery = searchQuery,
        )
        decimalColumn(
            column = StorageProductField.INCOMING,
            title = "Приход",
            valueOf = { it.incoming },
            searchQuery = searchQuery,
        )
        decimalColumn(
            column = StorageProductField.OUTGOING,
            title = "Расход",
            valueOf = { it.outgoing },
            searchQuery = searchQuery,
        )
        decimalColumn(
            column = StorageProductField.BALANCE_END,
            title = "Остаток",
            valueOf = { it.balanceOnEnd },
            searchQuery = searchQuery,
        )


    }

@Composable
private fun ExpandedHeader(
    areAllProductsExpanded: Boolean,
    onToggleExpandAll: () -> Unit,
) {
    ToolTipIconButton(
        tooltipText = if (areAllProductsExpanded) "Свернуть все" else "Развернуть все",
        onClick = onToggleExpandAll,
        icon = if (areAllProductsExpanded) {
            Res.drawable.arrow_upward
        } else {
            Res.drawable.arrow_downward
        },
    )
}

@Composable
private fun ExpandedCell(
    item: StorageProductUi,
    onToggleExpand: (productId: Int) -> Unit,
) {
    if (item.isProduct && !item.isExpanded) {
        ToolTipIconButton(
            tooltipText = "Развернуть",
            onClick = { onToggleExpand(item.productId) },
            icon = Res.drawable.arrow_downward,
        )
    }
    if (item.isProduct && item.isExpanded) {
        ToolTipIconButton(
            tooltipText = "Свернуть",
            onClick = { onToggleExpand(item.productId) },
            icon = Res.drawable.arrow_upward,
        )
    }
    if (!item.isProduct) {
        Text("")
    }
}

/** Добавляет штатную колонку наименования с кнопкой открытия карточки товара. */
private fun ReadonlyTableColumnsBuilder<StorageProductUi, StorageProductField, StorageTableData>.nameColumn(
    onOpenProduct: (productId: Int) -> Unit,
    searchQuery: () -> String,
) {
    column(key = StorageProductField.NAME, valueOf = { it.itemName }) {
        title { "Имя" }
        autoWidth()
        align(Alignment.Center)
        cell { item, _ ->
            if (item.isProduct) {
                NameRowWithSearchIcon(
                    text = item.itemName,
                    onClick = { onOpenProduct(item.productId) },
                    tooltipText = "Открыть товар",
                    searchQuery = searchQuery(),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    text = item.itemName,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        filter(TableFilterType.TextTableFilter())
    }
}

/** Добавляет центрированную числовую колонку с единым форматированием остатков. */
private fun ReadonlyTableColumnsBuilder<StorageProductUi, StorageProductField, StorageTableData>.decimalColumn(
    column: StorageProductField,
    title: String,
    valueOf: (StorageProductUi) -> Long,
    searchQuery: () -> String,
) {
    column(key = column, valueOf = valueOf) {
        title { title }
        autoWidth()
        align(Alignment.Center)
        cell { item, _ ->
            SearchHighlightedText(
                text = DecimalData3(valueOf(item)).toStartDoubleFormat(),
                query = searchQuery().takeIf { item.isProduct }.orEmpty(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                textAlign = TextAlign.Center,
            )
        }
    }

}
