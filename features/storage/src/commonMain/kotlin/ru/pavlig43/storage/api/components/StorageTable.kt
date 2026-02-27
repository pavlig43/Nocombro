package ru.pavlig43.storage.api.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.TableCustomization
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.DefaultStrings
import ru.pavlig43.storage.api.column.StorageColumn
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData

@OptIn(ExperimentalTableApi::class)
@Composable
fun StorageTable(
    state: TableState<StorageColumn>,
    tableData: StorageTableData,
    columns: ImmutableList<ColumnSpec<StorageProductUi, StorageColumn, StorageTableData>>,
    modifier: Modifier = Modifier,
) {
    EditableTable(
        itemsCount = tableData.displayedProducts.size,
        itemAt = { index -> tableData.displayedProducts.getOrNull(index) },
        state = state,
        tableData = tableData,
        columns = columns,
        strings = DefaultStrings,
        rowKey = { _, index -> index },
        rowEmbedded = { _, product ->
            val visible = product.expanded
            if (visible) {
                HorizontalDivider(
                    thickness = state.dimensions.dividerThickness,
                    modifier = Modifier.width(state.tableWidth),
                )
            }
            AnimatedVisibility(
                visible = visible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                StorageBatchesSection(product = product)
            }
        },
        onRowEditStart = { _, _ -> },
        onRowEditComplete = { true },
        onEditCancelled = { },
        modifier = modifier,
    )
}
