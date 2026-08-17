package ru.pavlig43.profitability.api.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.datetime.period.dateTime.DateTimeSelectorScreen
import ru.pavlig43.profitability.internal.component.LoadState
import ru.pavlig43.profitability.internal.component.ProfitabilityComponent
import ru.pavlig43.profitability.internal.component.ProfitabilityField
import ru.pavlig43.profitability.internal.component.createProfitabilityColumns
import ru.pavlig43.profitability.internal.model.ProfitabilityProduct
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.tablecore.state.rememberSaveableTableState
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ru.pavlig43.tablecore.ui.ScrollBar
import ua.wwind.table.ColumnSpec
import ua.wwind.table.Table
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState

@Composable
fun ProfitabilityScreen(component: ProfitabilityComponent) {
    Column(modifier = Modifier.fillMaxSize()) {
        DateTimeSelectorScreen(component.dateTimePeriodComponent)

        val loadState by component.loadState.collectAsState()
        when (val state = loadState) {
            is LoadState.Error -> ProfitabilityStateSurface(
                modifier = Modifier.weight(1f),
            ) {
                ErrorScreen(state.message)
            }

            is LoadState.Loading -> ProfitabilityStateSurface(
                modifier = Modifier.weight(1f),
            ) {
                LoadingUi()
            }

            is LoadState.Success -> {
                val columns = remember {
                    createProfitabilityColumns(
                        onToggleExpanded = component::onToggleDetailsExpanded,
                    )
                }
                val tableSettings = remember {
                    TableSettings(
                        showActiveFiltersHeader = true,
                        showFooter = true,
                        stripedRows = true,
                        enableDragToScroll = true,
                    )
                }
                val tableState = rememberSaveableTableState(
                    columns = ProfitabilityField.entries.toImmutableList(),
                    settings = tableSettings,
                    dimensions = TableDefaults.compactDimensions().copy(
                        rowHeight = 44.dp,
                        headerHeight = 48.dp,
                        footerHeight = 44.dp,
                    ),
                    initialSort = component.sort,
                )
                component.filters.forEach { (column, filterState) ->
                    tableState.filters[column] = filterState
                }
                LaunchedEffect(tableState) {
                    snapshotFlow { tableState.filters.toMap() }.collect(component::updateFilters)
                }
                LaunchedEffect(tableState) {
                    snapshotFlow { tableState.sort }.collect(component::updateSort)
                }

                val tableData by component.tableData.collectAsState()
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    ProfitabilitySummaryCard(
                        summary = state.data.summary,
                        modifier = Modifier.padding(
                            start = 24.dp,
                            top = 4.dp,
                            end = 24.dp,
                            bottom = 8.dp,
                        ),
                    )
                    ProfitabilityTable(
                        state = tableState,
                        tableData = tableData,
                        columns = columns,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfitabilityStateSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 16.dp),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@Composable
private fun ProfitabilityTable(
    state: TableState<ProfitabilityField>,
    tableData: ProfitabilityTableData,
    columns: ImmutableList<ColumnSpec<ProfitabilityProduct, ProfitabilityField, ProfitabilityTableData>>,
    modifier: Modifier = Modifier,
) {
    val verticalState = androidx.compose.foundation.lazy.rememberLazyListState()
    val horizontalState = androidx.compose.foundation.rememberScrollState()
    val stripedRowColor = lerp(
        MaterialTheme.colorScheme.surface,
        MaterialTheme.colorScheme.primaryContainer,
        0.16f,
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 1.dp,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Товары",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = productCountLabel(tableData.displayedProducts.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Table(
                    itemsCount = tableData.displayedProducts.size,
                    itemAt = { index -> tableData.displayedProducts.getOrNull(index) },
                    state = state,
                    columns = columns,
                    tableData = tableData,
                    strings = RussianStringProvider,
                    verticalState = verticalState,
                    horizontalState = horizontalState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 12.dp, bottom = 12.dp),
                    colors = TableDefaults.colors(
                        headerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                        rowContainerColor = MaterialTheme.colorScheme.surface,
                        rowSelectedContainerColor = stripedRowColor,
                        stripedRowContainerColor = stripedRowColor,
                        footerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    shape = RectangleShape,
                    border = TableDefaults.NoBorder,
                    rowKey = { item, index -> item?.productId ?: index },
                    rowEmbedded = { _, product ->
                        ExpandedBatchDetails(
                            product = product,
                            modifier = Modifier.width(state.tableWidth),
                        )
                    },
                )
                ScrollBar(
                    verticalState = verticalState,
                    horizontalState = horizontalState,
                    subtle = true,
                )
            }
        }
    }
}

@Composable
private fun ExpandedBatchDetails(
    product: ProfitabilityProduct,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = product.expandedDetails,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        BatchDetailsTable(
            product = product,
            modifier = modifier,
        )
    }
}

private fun productCountLabel(count: Int): String {
    val remainder100 = count % 100
    val remainder10 = count % 10
    val noun = when {
        remainder100 in (11..14) -> "позиций"
        remainder10 == 1 -> "позиция"
        remainder10 in (2..4) -> "позиции"
        else -> "позиций"
    }
    return "$count $noun"
}
