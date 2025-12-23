package ru.pavlig43.itemlist.internal.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ru.pavlig43.itemlist.api.model.IItemUi
import ru.pavlig43.itemlist.internal.component.ImmutableTableComponent
import ru.pavlig43.itemlist.internal.component.ItemListState
import ru.pavlig43.itemlist.internal.component.SelectionUiEvent
import ru.pavlig43.itemlist.internal.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.sample.app.components.SelectionActionBar
import ua.wwind.table.state.SortState
import ua.wwind.table.state.rememberTableState

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : IItemUi, C> ImmutableTableBox(
    component: ImmutableTableComponent<*, I, C>,
    modifier: Modifier = Modifier
) {
    val itemListState by component.itemListState.collectAsState()

    val tableData: TableData<I> by component.tableData.collectAsState()

    Box(modifier.padding(16.dp)) {

        when (val state = itemListState) {

            is ItemListState.Error -> ErrorScreen(state.message)
            is ItemListState.Loading -> LoadingScreen()
            is ItemListState.Success -> {
                ImmutableTable(
                    columns = component.columns,
                    items = tableData.displayedItems,
                    onRowClick = { component.onItemClick(it) },
                    tableData = tableData,
                    onFiltersChanged = component::updateFilters,
                    onSortChanged = component::updateSort,
                    onEvent = component::onEvent
                )
            }

        }
    }
}

@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTableApi::class)
@Composable
private fun <I : IItemUi, C, E : TableData<I>> ImmutableTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    items: List<I>,
    onFiltersChanged: (Map<C, TableFilterState<*>>) -> Unit,
    onEvent: (SelectionUiEvent) -> Unit,
    onSortChanged: (SortState<C>?) -> Unit,
    onRowClick: (I) -> Unit,
    tableData: E
) {

    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = TableSettings(
            stripedRows = true,
            autoApplyFilters = true,
            showFastFilters = true,
            showActiveFiltersHeader = true,
            selectionMode = SelectionMode.Multiple,
            pinnedColumnsCount = 3
        )
    )
    LaunchedEffect(state) {
        snapshotFlow { state.filters.toMap() }.collect { filters -> onFiltersChanged(filters) }
    }

    LaunchedEffect(state) { snapshotFlow { state.sort }.collect { sort -> onSortChanged(sort) } }

    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()
    Box {

        Table(
            itemsCount = items.size,
            itemAt = { index -> items.getOrNull(index) },
            state = state,
            strings = RussianStringProvider,
            customization = DefaultTableCustomization(),
            tableData = tableData,
            columns = columns,
            verticalState = verticalState,
            horizontalState = horizontalState,
            onRowClick = onRowClick,
            modifier = Modifier.fillMaxSize()
                .padding(end = 16.dp, bottom = 16.dp)
        )
        SelectionActionBar(
            selectedCount = tableData.selectedIds.size,
            onDeleteClick = {
                onEvent(SelectionUiEvent.DeleteSelected)
            },
            onClearSelection = {
                onEvent(SelectionUiEvent.ClearSelection)
            },
//                                liquidState = liquidState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
        ScrollBar(verticalState, horizontalState)

    }

}
@Composable
internal expect fun BoxScope.ScrollBar(
    verticalState: LazyListState,
    horizontalState: ScrollState
)