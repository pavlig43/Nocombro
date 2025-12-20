package ru.pavlig43.itemlist.internal.model

import androidx.compose.runtime.Immutable
import ru.pavlig43.itemlist.api.model.IItemUi

@Immutable
internal data class TableData<I: IItemUi>(

    val displayedItems: List<I> = emptyList(),
    /** IDs of selected  */
    val selectedIds: Set<Int> = emptySet(),

    /** Whether selection mode is enabled */

    val isSelectionMode: Boolean,
)