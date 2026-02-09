package ru.pavlig43.mutable.api.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.mutable.api.component.singleLine.SingleLineComponent
import ru.pavlig43.tablecore.model.ITableUi
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.config.RowHeightMode
import ua.wwind.table.config.TableSettings
import ua.wwind.table.state.rememberTableState

@OptIn(ExperimentalTableApi::class)
@Composable
fun <I : ITableUi, C> SingleLineScreen(
    component: SingleLineComponent<*, I, C>,
    modifier: Modifier = Modifier,
) {
    LoadInitDataScreen(component.initDataComponent) {
        val items by component.itemFields.collectAsState()
        val defaultTableSettings = TableSettings(
            autoApplyFilters = false,
            editingEnabled = true,
            rowHeightMode = RowHeightMode.Dynamic,
            enableTextSelection = false,
            pinnedColumnsCount = 0,
            showFooter = false,
            showFastFilters = false,
            showActiveFiltersHeader = false,
            enableDragToScroll = false
        )
        val state = rememberTableState(
            columns = component.columns.map { it.key }.toImmutableList(),
            settings = defaultTableSettings
        )
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            EditableTable(
                itemsCount = items.size,
                itemAt = { index -> items.getOrNull(index) },
                state = state,
                columns = component.columns,
                tableData = Unit,
            )
        }
    }
}