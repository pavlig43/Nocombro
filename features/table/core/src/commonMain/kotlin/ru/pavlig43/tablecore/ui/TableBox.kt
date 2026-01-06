package ru.pavlig43.tablecore.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.StringProvider


@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTableApi::class)
@Composable
fun <I : ITableUi, C, E : TableData<I>> TableBox(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    onFiltersChanged: (Map<C, TableFilterState<*>>) -> Unit,
    onSortChanged: (SortState<C>?) -> Unit,
    table:@Composable BoxScope.(
        verticalState: LazyListState,
        horizontalState: ScrollState,
        tableState: TableState<C>,
        stringProvider: StringProvider,
        modifier: Modifier
    ) -> Unit,
) {

    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = TableSettings(
            stripedRows = true,
            autoApplyFilters = true,
            showFastFilters = true,
            showActiveFiltersHeader = true,
            editingEnabled = true,
            enableTextSelection = true,
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
        table(
            verticalState,
            horizontalState,
            state,
            RussianStringProvider,
            Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 16.dp))

        ScrollBar(verticalState, horizontalState)

    }

}
@Composable
internal expect fun BoxScope.ScrollBar(
    verticalState: LazyListState,
    horizontalState: ScrollState
)