package ru.pavlig43.storage.api.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.storage.api.column.createStorageBatchColumns
import ru.pavlig43.storage.internal.model.StorageBatchUi
import ru.pavlig43.storage.internal.model.StorageProductUi
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.DefaultStrings

@OptIn(ExperimentalTableApi::class)
@Composable
fun StorageBatchesSection(
    product: StorageProductUi,
    modifier: Modifier = Modifier,
) {
    val columns = remember { createStorageBatchColumns() }
    val batchSettings = remember {
        TableSettings(
            isDragEnabled = false,
            autoApplyFilters = false,
            showFastFilters = false,
            autoFilterDebounce = 0,
            stripedRows = false,
            showActiveFiltersHeader = false,
            selectionMode = SelectionMode.None,
            rowHeightMode = RowHeightMode.Dynamic,
            enableDragToScroll = false,
            showFooter = false,
        )
    }
    val batchState = rememberTableState(
        columns = ru.pavlig43.storage.api.column.StorageBatchColumn.entries.toImmutableList(),
        settings = batchSettings,
        dimensions = TableDefaults.compactDimensions(),
    )

    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 8.dp).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Партии",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )

        Table(
            itemsCount = product.batches.size,
            itemAt = { index -> product.batches.getOrNull(index) },
            state = batchState,
            tableData = product,
            columns = columns,
            strings = DefaultStrings,
            rowKey = { item, index -> item?.composeId ?: index },
            modifier = Modifier.padding(top = 8.dp),
            embedded = true,
        )
    }
}
