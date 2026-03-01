package ru.pavlig43.storage.api.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.storage.api.column.StorageProductField
import ua.wwind.table.ColumnSpec
import ua.wwind.table.EditableTable
import ua.wwind.table.ExperimentalTableApi
import ua.wwind.table.state.TableState
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.storage.internal.model.StorageTableData
import ru.pavlig43.tablecore.ui.RussianStringProvider
import ua.wwind.table.Table
import ru.pavlig43.tablecore.ui.ScrollBar

@OptIn(ExperimentalTableApi::class)
@Composable
internal fun StorageTable(
    state: TableState<StorageProductField>,
    tableData: StorageTableData,
    columns: ImmutableList<ColumnSpec<StorageProductUi, StorageProductField, StorageTableData>>,
    modifier: Modifier = Modifier,
) {
    val verticalState = rememberLazyListState()
    val horizontalState = rememberScrollState()
    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Table(
            itemsCount = tableData.displayedProducts.size,
            itemAt = { index -> tableData.displayedProducts.getOrNull(index) },
            state = state,
            columns = columns,
            tableData = tableData,
            onRowClick = { },
            strings = RussianStringProvider,
            verticalState = verticalState,
            horizontalState = horizontalState,
            modifier = modifier,
        )
        ScrollBar(
            verticalState = verticalState,
            horizontalState = horizontalState,
        )

    }
}
