package ru.pavlig43.tablecore.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState
import ua.wwind.table.state.TableState
import ua.wwind.table.state.rememberTableState
import ua.wwind.table.strings.StringProvider
import ua.wwind.table.tableColumns


@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTableApi::class)
@Composable
fun <I : ITableUi, C, E : TableData<I>> TableBox(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    onFiltersChanged: (Map<C, TableFilterState<*>>) -> Unit,
    onSortChanged: (SortState<C>?) -> Unit,
    table: @Composable BoxScope.(
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
            Modifier.fillMaxWidth().padding(end = 16.dp, bottom = 16.dp)
        )
        ScrollBar(verticalState, horizontalState)

    }

}

@Composable
internal expect fun BoxScope.ScrollBar(
    verticalState: LazyListState,
    horizontalState: ScrollState
)

enum class ExampleField {
    DATE
}

data class ExampleData(
    val date: Long
)

@OptIn(ExperimentalTableApi::class)
@Composable
fun Example() {
    val items = mutableStateListOf(ExampleData(0L))
    val datePickerState = rememberDatePickerState()
    datePickerState.selectedDateMillis?.let {
        items[0] = items[0].copy(date = it)
    }
    var isVisibleDialog by remember { mutableStateOf(false) }
    val columns = tableColumns<ExampleData, ExampleField, Unit> {
        column(ExampleField.DATE, { it.date }) {
            header { Text("DATE") }
            cell { data, _ ->
                Row(Modifier.fillMaxWidth()) {
                    Text(data.date.toString())
                    IconButton({ isVisibleDialog = !isVisibleDialog }) {
                        Icon(Icons.Default.Search, null)
                    }
                }
            }
        }
    }
    if (isVisibleDialog) {
        Dialog(onDismissRequest = {isVisibleDialog = false}){
            TextField(
                value = "",
                onValueChange = {}
            )
//            DatePicker(
//                state = datePickerState
//            )
        }
    }
    val state = rememberTableState(
        columns = columns.map { it.key }.toImmutableList(),
        settings = TableSettings(
            enableTextSelection = true,

            )
    )
    EditableTable(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = state,
        columns = columns,
        tableData = Unit
    )

}
