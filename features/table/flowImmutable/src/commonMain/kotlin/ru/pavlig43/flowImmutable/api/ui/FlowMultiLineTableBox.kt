package ru.pavlig43.flowImmutable.api.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.flowImmutable.api.component.FlowMultiLineEvent
import ru.pavlig43.flowImmutable.api.component.FlowMultilineComponent
import ru.pavlig43.flowImmutable.api.component.ItemListState
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.TableBox
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.close
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@OptIn(ExperimentalTableApi::class)
@Composable
fun <I : IMultiLineTableUi, C> FlowMultiLineTableBox(
    component: FlowMultilineComponent<*, *, I, C>,
    modifier: Modifier = Modifier
) {

    LoadInitDataScreen(component.initDataComponent) {
        val itemListState by component.itemListState.collectAsState()

        val tableData: TableData<I> by component.tableData.collectAsState()

        Box(modifier.padding(16.dp)) {

            when (val state = itemListState) {

                is ItemListState.Error -> ErrorScreen(state.message)
                is ItemListState.Loading -> LoadingUi()
                is ItemListState.Success -> {
                    TableBox(
                        columns = component.columns,
                        onFiltersChanged = component::updateFilters,
                        onSortChanged = component::updateSort,
                    ) { verticalState, horizontalState, tableState, stringProvider, modifier ->
                        FlowMultiLineTable(
                            columns = component.columns,
                            tableState = tableState,
                            stringProvider = stringProvider,
                            verticalState = verticalState,
                            horizontalState = horizontalState,
                            items = tableData.displayedItems,
                            onEvent = component::onEvent,
                            tableData = tableData,
                            modifier = modifier
                        )

                    }

                }

            }
        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTableApi::class)
@Composable
private fun <I : IMultiLineTableUi, C, E : TableData<I>> BoxScope.FlowMultiLineTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    tableState: TableState<C>,
    stringProvider: StringProvider,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    items: List<I>,
    onEvent: (FlowMultiLineEvent) -> Unit,
    tableData: E,
    modifier: Modifier
) {

    Table(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = tableState,
        strings = stringProvider,
        onRowClick = { onEvent(FlowMultiLineEvent.RowClick(it)) },
        customization = DefaultTableCustomization(),
        tableData = tableData,
        columns = columns,
        verticalState = verticalState,
        horizontalState = horizontalState,
        modifier = modifier
    )
    SelectionActionBar(
        selectedCount = tableData.selectedIds.size,
        onDeleteClick = {
            onEvent(FlowMultiLineEvent.DeleteSelected)
        },
        onClearSelection = {
            onEvent(FlowMultiLineEvent.Selection(SelectionUiEvent.ClearSelection))
        },
//                                liquidState = liquidState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
    )
}

/**
 * Floating action bar shown at the bottom when items are selected.
 * Displays the count of selected items and provides delete/clear actions.
 * Features a Liquid Glass effect powered by the Liquid library with GPU-accelerated shaders.
 *
 * @param selectedCount Number of selected items to display
 * @param onDeleteClick Callback when delete button is clicked
 * @param onClearSelection Callback when clear selection button is clicked
 * @param modifier Modifier for the composable
 */
@Composable
private fun SelectionActionBar(
    selectedCount: Int,
    onDeleteClick: () -> Unit,
    onClearSelection: () -> Unit,
//    liquidState: LiquidState,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = selectedCount > 0,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        // Liquid Glass effect: GPU-accelerated shader distortion with semi-transparent background
        Surface(
            modifier = Modifier,
//                .liquid(liquidState),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
            shape = RoundedCornerShape(24.dp),
            border =
                BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            ) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = "Clear selection",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "$selectedCount selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDeleteClick,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.close),
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    Text("Delete")
                }
            }
        }
    }
}