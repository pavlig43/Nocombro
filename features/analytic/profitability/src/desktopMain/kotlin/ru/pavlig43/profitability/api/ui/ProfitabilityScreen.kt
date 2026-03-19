package ru.pavlig43.profitability.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
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
import ru.pavlig43.profitability.internal.model.ProfitabilityTableData
import ru.pavlig43.profitability.internal.model.ProfitabilityUi
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ru.pavlig43.tablecore.ui.ScrollBar
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.TableDefaults
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState

@Composable
fun ProfitabilityScreen(component: ProfitabilityComponent) {
    DateTimeSelectorScreen(component.dateTimePeriodComponent)

    val loadState by component.loadState.collectAsState()
    when (val state = loadState) {
        is LoadState.Error -> ErrorScreen(state.message)
        is LoadState.Loading -> LoadingUi()
        is LoadState.Success -> {
            val columns = remember { createProfitabilityColumns() }
            val tableSettings = remember {
                TableSettings(showActiveFiltersHeader = true)
            }
            val tableState = rememberTableState(
                columns = ProfitabilityField.entries.toImmutableList(),
                settings = tableSettings,
            )
            LaunchedEffect(tableState) {
                snapshotFlow { tableState.filters.toMap() }.collect { filters ->
                    component.updateFilters(filters)
                }
            }
            val tableData by component.tableData.collectAsState()

            ProfitabilityTable(
                state = tableState,
                tableData = tableData,
                columns = columns
            )
        }
    }
}

@OptIn(ExperimentalTableApi::class)
@Composable
private fun ProfitabilityTable(
    state: TableState<ProfitabilityField>,
    tableData: ProfitabilityTableData,
    columns: ImmutableList<ColumnSpec<ru.pavlig43.profitability.internal.model.ProfitabilityUi, ProfitabilityField, ProfitabilityTableData>>,
    modifier: Modifier = Modifier
) {
    val verticalState = androidx.compose.foundation.lazy.rememberLazyListState()
    val horizontalState = androidx.compose.foundation.rememberScrollState()

    Box(
        modifier = modifier.padding(start = 24.dp)
    ) {
        Table(
            itemsCount = tableData.displayedProducts.size,
            itemAt = { index -> tableData.displayedProducts.getOrNull(index) },
            state = state,
            columns = columns,
            tableData = tableData,
            strings = RussianStringProvider,
            verticalState = verticalState,
            horizontalState = horizontalState,
            modifier = modifier,
            colors = TableDefaults.colors(
                headerContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        )
        ScrollBar(
            verticalState = verticalState,
            horizontalState = horizontalState
        )
    }
}
