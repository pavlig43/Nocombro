package ru.pavlig43.storage.api

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.storage.api.component.LoadState
import ru.pavlig43.storage.api.component.StorageComponent
import ru.pavlig43.storage.api.components.StorageTable
import ru.pavlig43.storage.api.column.createStorageColumns
import ua.wwind.table.state.rememberTableState

@Composable
fun StorageScreen(
    component: StorageComponent
){
    val loadState by component.loadState.collectAsState()
    when(val state = loadState) {
        is LoadState.Error -> ErrorScreen(state.message)
        is LoadState.Loading -> LoadingUi()
        is LoadState.Success -> {
            val columns = remember { createStorageColumns(component::toggleExpand) }
            val tableState = rememberTableState(
                columns = ru.pavlig43.storage.api.column.StorageColumn.entries.toImmutableList(),
            )
            val tableData by component.tableData.collectAsState()

            StorageTable(
                state = tableState,
                tableData = tableData,
                columns = columns,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}