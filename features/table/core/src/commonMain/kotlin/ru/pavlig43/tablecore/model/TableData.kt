package ru.pavlig43.tablecore.model

import androidx.compose.runtime.Immutable
import ru.pavlig43.tablecore.model.ITableUi

@Immutable
data class TableData<I: ITableUi>(

    val displayedItems: List<I> = emptyList(),
    /** IDs of selected  */
    val selectedIds: Set<Int> = emptySet(),

    /** Whether selection mode is enabled */

    val isSelectionMode: Boolean,
)
