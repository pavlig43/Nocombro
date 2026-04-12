package ru.pavlig43.tablecore.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.RowHeightMode
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
fun <I : IMultiLineTableUi, C, E : TableData<I>> TableBox(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    onFiltersChanged: (Map<C, TableFilterState<*>>) -> Unit,
    onSortChanged: (SortState<C>?) -> Unit,
    initialSort: SortState<C>? = null,
    initialFilters: Map<C, TableFilterState<*>> = emptyMap(),
    tableSettingsModify: (TableSettings) -> TableSettings = { it },
    table: @Composable BoxScope.(
        verticalState: LazyListState,
        horizontalState: ScrollState,
        tableState: TableState<C>,
        stringProvider: StringProvider,
        modifier: Modifier
    ) -> Unit,

    ) {
    val defaultTableSettings = TableSettings(
        stripedRows = true,
        autoApplyFilters = true,
        showActiveFiltersHeader = true,
        editingEnabled = true,
        rowHeightMode = RowHeightMode.Fixed,
        showFooter = true,
        enableTextSelection = true,
        enableDragToScroll = true,
        selectionMode = SelectionMode.Multiple,
        pinnedColumnsCount = 0
    )

    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = tableSettingsModify(defaultTableSettings),
        initialSort = initialSort,
    )
    initialFilters.forEach { (column, filterState) ->
        state.filters[column] = filterState
    }
    LaunchedEffect(state) {
        snapshotFlow { state.filters.toMap() }.collect { filters -> onFiltersChanged(filters) }
    }

    LaunchedEffect(state) {
        snapshotFlow { state.sort }.collect { sort -> onSortChanged(sort) }
    }

    val coroutineScope = rememberCoroutineScope()
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()
    Box(
        modifier = Modifier.pointerInput(verticalState) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    if (event.type == PointerEventType.Scroll) {
                        val scrollDelta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                        if (scrollDelta != 0f) {
                            // Ловим прокрутку колесом на всем контейнере таблицы,
                            // а не только когда курсор находится прямо над сеткой.
                            coroutineScope.launch {
                                verticalState.scrollBy(-scrollDelta * 64f)
                            }
                        }
                    }
                }
            }
        }
    ) {
        table(
            verticalState,
            horizontalState,
            state,
            RussianStringProvider,
            Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 16.dp)
        )
        ScrollBar(verticalState, horizontalState)

    }

}

