package ru.pavlig43.immutable.internal.component.items.batchMovement

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object BatchMovementFilterMatcher : FilterMatcher<BatchMovementTableUi, BatchMovementField>() {
    override fun matchesRules(
        item: BatchMovementTableUi,
        column: BatchMovementField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            BatchMovementField.DATETIME -> true
            BatchMovementField.BATCH_NAME -> matchesTextField(item.batchName, stateAny)
            BatchMovementField.PRODUCT_NAME -> matchesTextField(item.productName, stateAny)
            BatchMovementField.BALANCE_BEFORE -> matchesTextField(item.balanceBeforeStart.toString(), stateAny)
            BatchMovementField.INCOMING -> matchesTextField(item.incoming.toString(), stateAny)
            BatchMovementField.OUTGOING -> matchesTextField(item.outgoing.toString(), stateAny)
            BatchMovementField.BALANCE_END -> matchesTextField(item.balanceOnEnd.toString(), stateAny)
        }
    }
}
