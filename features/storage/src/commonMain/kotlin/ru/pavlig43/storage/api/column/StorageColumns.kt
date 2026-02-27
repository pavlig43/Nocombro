package ru.pavlig43.storage.api.column

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.mutable.api.column.DecimalFormat
import ru.pavlig43.mutable.api.column.readDecimalColumnWithFooter
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.arrow_downward
import ru.pavlig43.theme.arrow_upward
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTableColumnsBuilder
import ua.wwind.table.editableTableColumns

fun createStorageColumns(
    onToggleExpand: (productId: Int) -> Unit
): ImmutableList<ColumnSpec<StorageProductUi, StorageColumn, StorageTableData>> =
    editableTableColumns {
        val iconButtonSize = 48.dp

        column(StorageColumn.EXPAND, valueOf = { it.expanded }) {
            title { "Партии" }
            width(iconButtonSize, iconButtonSize)
            resizable(false)
            cell { item, _ ->
                IconButton(
                    onClick = { onToggleExpand(item.productId) },
                    modifier = Modifier.size(iconButtonSize),
                ) {
                    if (item.expanded) {
                        Icon(
                            painterResource(Res.drawable.arrow_upward),
                            contentDescription = "Свернуть партии",
                        )
                    } else {
                        Icon(
                            painterResource(Res.drawable.arrow_downward),
                            contentDescription = "Развернуть партии",
                        )
                    }
                }
            }
        }

        readTextColumn(
            key = StorageColumn.PRODUCT_NAME,
            getValue = { it.productName },
            headerText = "Продукт"
        )

        readDecimalColumnWithFooter(
            key = StorageColumn.BALANCE_BEFORE,
            getValue = { it.balanceBeforeStart },
            headerText = "Остаток на начало (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            footerValue = { tableData ->
                tableData.displayedProducts.sumOf { it.balanceBeforeStart }
            },
            isSortable = false
        )

        readDecimalColumnWithFooter(
            key = StorageColumn.INCOMING,
            getValue = { it.incoming },
            headerText = "Приход (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            footerValue = { tableData ->
                tableData.displayedProducts.sumOf { it.incoming }
            },
            isSortable = false
        )

        readDecimalColumnWithFooter(
            key = StorageColumn.OUTGOING,
            getValue = { it.outgoing },
            headerText = "Расход (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            footerValue = { tableData ->
                tableData.displayedProducts.sumOf { it.outgoing }
            },
            isSortable = false
        )

        readDecimalColumnWithFooter(
            key = StorageColumn.BALANCE_END,
            getValue = { it.balanceOnEnd },
            headerText = "Остаток на конец (кг)",
            decimalFormat = DecimalFormat.Decimal3(),
            footerValue = { tableData ->
                tableData.displayedProducts.sumOf { it.balanceOnEnd }
            },
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
