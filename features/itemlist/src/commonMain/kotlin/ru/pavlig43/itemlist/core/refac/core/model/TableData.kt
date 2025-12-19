package ru.pavlig43.itemlist.core.refac.core.model

import androidx.compose.runtime.Immutable
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi

@Immutable
data class TableData<I: IItemUi>(

    val displayedItems: List<I> = emptyList(),
    /** IDs of selected  */
    val selectedIds: Set<Int> = emptySet(),

    /** Whether selection mode is enabled */

    val isSelectionMode: Boolean,
)





