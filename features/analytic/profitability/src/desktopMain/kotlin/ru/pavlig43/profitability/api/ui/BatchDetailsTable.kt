package ru.pavlig43.profitability.api.ui

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
import ru.pavlig43.profitability.internal.component.BatchDetailsField
import ru.pavlig43.profitability.internal.component.createBatchDetailsColumns
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun BatchDetailsTable(
    product: ProfitabilityProduct,
    modifier: Modifier = Modifier,
) {
    val columns = remember { createBatchDetailsColumns() }
    val tableSettings = remember {
        TableSettings(
            isDragEnabled = false,
            autoApplyFilters = false,
            showFastFilters = false,
            stripedRows = false,
            showActiveFiltersHeader = false,
            selectionMode = SelectionMode.None,
            rowHeightMode = RowHeightMode.Dynamic,
            enableDragToScroll = false,
            showFooter = true,
        )
    }
    val tableState = rememberTableState(
        columns = BatchDetailsField.entries.toImmutableList(),
        settings = tableSettings,
        dimensions = TableDefaults.compactDimensions(),
    )

    Column(
        modifier = modifier.padding(top = 8.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Детали по партиям",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Table(
            itemsCount = product.details.size,
            itemAt = { index -> product.details.getOrNull(index) },
            state = tableState,
            tableData = ProfitabilityTableData(listOf(product)),
            columns = columns,
            strings = RussianStringProvider,
            embedded = true
        )
    }
}
