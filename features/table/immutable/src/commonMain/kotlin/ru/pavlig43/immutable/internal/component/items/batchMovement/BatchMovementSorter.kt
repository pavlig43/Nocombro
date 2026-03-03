package ru.pavlig43.immutable.internal.component.items.batchMovement

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object BatchMovementSorter : SortMatcher<BatchMovementTableUi, BatchMovementField> {
    override fun sort(
        items: List<BatchMovementTableUi>,
        sort: SortState<BatchMovementField>?,
    ): List<BatchMovementTableUi> {
        if (sort == null) return items

        val sortedList = when (sort.column) {
            BatchMovementField.DATETIME -> items.sortedBy { it.movementDate }
            BatchMovementField.BATCH_NAME -> items.sortedBy { it.batchName.lowercase() }
            BatchMovementField.PRODUCT_NAME -> items.sortedBy { it.productName.lowercase() }
            BatchMovementField.BALANCE_BEFORE -> items.sortedBy { it.balanceBeforeStart }
            BatchMovementField.INCOMING -> items.sortedBy { it.incoming }
            BatchMovementField.OUTGOING -> items.sortedBy { it.outgoing }
            BatchMovementField.BALANCE_END -> items.sortedBy { it.balanceOnEnd }
        }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
