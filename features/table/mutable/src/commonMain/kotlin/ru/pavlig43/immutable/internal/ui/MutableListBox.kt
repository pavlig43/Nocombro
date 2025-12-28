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
import ru.pavlig43.tablecore.model.ITableUi
import ru.pavlig43.immutable.internal.component.MutableTableComponent
import ru.pavlig43.immutable.internal.component.MutableUiEvent
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.ui.TableBox
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.state.TableState
import ua.wwind.table.strings.StringProvider

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : ITableUi, C> MutableTableBox(
    component: MutableTableComponent<*, I, C, *>,
    modifier: Modifier = Modifier
) {


    LoadInitDataScreen(component.initDataComponent) {

        val tableData: TableData<I> by component.tableData.collectAsState()

        Box(modifier.padding(16.dp)) {


            TableBox(
                columns = component.columns,
                onFiltersChanged = component::updateFilters,
                onSortChanged = component::updateSort,
            ) { verticalState, horizontalState, tableState, stringProvider, modifier ->
                MutableTable(
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


@Suppress("LongParameterList", "LongMethod")
@OptIn(ExperimentalTableApi::class)
@Composable
private fun <I : ITableUi, C, E : TableData<I>> BoxScope.MutableTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    tableState: TableState<C>,
    stringProvider: StringProvider,
    verticalState: LazyListState,
    horizontalState: ScrollState,
    items: List<I>,
    onEvent: (MutableUiEvent) -> Unit,
    tableData: E,
    modifier: Modifier
) {

    EditableTable(
        itemsCount = items.size,
        itemAt = { index -> items.getOrNull(index) },
        state = tableState,
        strings = stringProvider,
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
            onEvent(MutableUiEvent.DeleteSelected)
        },
        onClearSelection = {
            onEvent(MutableUiEvent.Selection(SelectionUiEvent.ClearSelection))
        },
//                                liquidState = liquidState,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(16.dp),
    )


}

