package ru.pavlig43.immutable.internal.component.items.batch

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object BatchFilterMatcher : FilterMatcher<BatchTableUi, BatchField>() {

    override fun matchesRules(
        item: BatchTableUi,
        column: BatchField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {
                BatchField.SELECTION -> true
                BatchField.ID -> true
                BatchField.BATCH_ID -> matchesTextField(item.batchId.toString(), stateAny)
                BatchField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
                BatchField.COUNT -> matchesTextField(item.balance.toString(), stateAny)
                BatchField.DATE_BORN -> matchesTextField(item.dateBorn.toString(), stateAny)
            }
        return matches
    }
}
