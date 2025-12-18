package ru.pavlig43.itemlist.refactor

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingScreen
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.Table
import ua.wwind.table.config.DefaultTableCustomization
import ua.wwind.table.config.SelectionMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState




@OptIn(ExperimentalTableApi::class)
@Composable
fun DocScreen1(
    component: DocumentTableComponent,
    modifier: Modifier = Modifier
) {

    ImmutableListScreen1(
        component = component,
        columns = createColumn(component.withCheckbox,component::onEvent,),
        onItemClick = { component.onItemClick(it) },
        modifier = modifier
    )

}

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun <I : Any, C> ImmutableListScreen1(
    component: IImmutableTableComponent<I>,
    columns: ImmutableList<ColumnSpec<I, C, TableData<I>>>,
    onItemClick: (I) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemListState by component.itemListState.collectAsState()

    val tableData: TableData<I> by component.tableData.collectAsState()
    Box(modifier.padding(16.dp)) {

        when (val state = itemListState) {

            is ItemListState1.Error -> ErrorScreen(state.message)
            is ItemListState1.Loading -> LoadingScreen()
            is ItemListState1.Success -> {
                ImmutableListTable(
                    columns = columns,
                    items = tableData.displayedItems,
                    onRowClick = onItemClick,
                    tableData = tableData
                )
            }

        }
    }
}


@OptIn(ExperimentalTableApi::class)
@Composable
private fun <I : Any, C, E: TableData<I>> ImmutableListTable(
    columns: ImmutableList<ColumnSpec<I, C, E>>,
    items: List<I>,
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
            pinnedColumnsCount = 1
        )
    )
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()
    Box {

        Table(
            itemsCount = items.size,
            itemAt = { index -> items.getOrNull(index) },
            state = state,
            customization = DefaultTableCustomization(),
            tableData = tableData,
            columns = columns,
            verticalState = verticalState,
            horizontalState = horizontalState,
            onRowClick = onRowClick,
            modifier = Modifier.fillMaxSize()
                .padding(end = 16.dp, bottom = 16.dp)
        )

        val lineColor = MaterialTheme.colorScheme.secondary
        val style = LocalScrollbarStyle.current.copy(
            unhoverColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.7f),
            hoverColor = MaterialTheme.colorScheme.onSecondary,
        )
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(verticalState),
            style = style,
            modifier = Modifier.align(Alignment.CenterEnd).padding(bottom = 24.dp)
                .background(lineColor)
        )

        // Horizontal scrollbar
        HorizontalScrollbar(
            adapter = rememberScrollbarAdapter(horizontalState),
            style = style,
            modifier = Modifier.align(Alignment.BottomStart).padding(end = 24.dp)
                .background(lineColor)
        )
    }

}