package ru.pavlig43.immutable.internal.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.immutable.internal.component.ImmutableTableComponent
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.immutable.internal.component.ItemListState
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.TableBox
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : IMultiLineTableUi, C> ImmutableTableBox(
    component: ImmutableTableComponent<*, I, C>,
    modifier: Modifier = Modifier
) {
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
                    ImmutableTable(
                        columns = component.columns,
                        tableState = tableState,
                        stringProvider = stringProvider,
                        verticalState = verticalState,
                        horizontalState = horizontalState,
                        items = tableData.displayedItems,
                        onEvent = component::onEvent,
                        onRowClick = { component.onItemClick(it) },
                        tableData = tableData,
                        modifier = modifier
                    )

                }

            }

        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTableApi::class)
@Composable
private fun <I : IMultiLineTableUi, C, E : TableData<I>> BoxScope.ImmutableTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    tableState: TableState<C>,
    stringProvider: StringProvider,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    items: List<I>,
    onEvent: (ImmutableTableUiEvent) -> Unit,
    onRowClick: (I) -> Unit,
    tableData: E,
    modifier: Modifier
) {

    Table(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = tableState,
        strings = stringProvider,
        customization = DefaultTableCustomization(),
        tableData = tableData,
        columns = columns,
        verticalState = verticalState,
        horizontalState = horizontalState,
        onRowClick = onRowClick,
        modifier = modifier
    )
    SelectionActionBar(
        selectedCount = tableData.selectedIds.size,
        onDeleteClick = {
            onEvent(ImmutableTableUiEvent.DeleteSelected)
        },
        onClearSelection = {
            onEvent(ImmutableTableUiEvent.Selection(SelectionUiEvent.ClearSelection))
        },
//                                liquidState = liquidState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
    )


}
