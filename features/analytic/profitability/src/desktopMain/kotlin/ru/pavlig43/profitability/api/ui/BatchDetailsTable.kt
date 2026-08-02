package ru.pavlig43.profitability.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.profitability.internal.component.BatchDetailsField
import ru.pavlig43.profitability.internal.component.createBatchDetailsColumns
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.tablecore.state.rememberSaveableTableState
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings

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
            stripedRows = true,
            showActiveFiltersHeader = false,
            selectionMode = SelectionMode.None,
            rowHeightMode = RowHeightMode.Dynamic,
            enableDragToScroll = true,
        )
    }
    val tableState = key(product.productId) {
        rememberSaveableTableState(
            columns = BatchDetailsField.entries.toImmutableList(),
            settings = tableSettings,
            dimensions = TableDefaults.compactDimensions(),
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 48.dp, top = 8.dp, end = 12.dp, bottom = 8.dp),
        shape = RoundedCornerShape(14.dp),
        color = lerp(
            MaterialTheme.colorScheme.surfaceContainerLow,
            MaterialTheme.colorScheme.primaryContainer,
            0.06f,
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Партии",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "· ${product.details.size}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
            Table(
                itemsCount = product.details.size,
                itemAt = { index -> product.details.getOrNull(index) },
                state = tableState,
                tableData = ProfitabilityTableData(listOf(product)),
                columns = columns,
                strings = RussianStringProvider,
                embedded = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TableDefaults.colors(
                    headerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    rowContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    stripedRowContainerColor = lerp(
                        MaterialTheme.colorScheme.surfaceContainerLow,
                        MaterialTheme.colorScheme.primaryContainer,
                        0.12f,
                    ),
                    footerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                shape = RectangleShape,
                border = TableDefaults.NoBorder,
            )
        }
    }
}
