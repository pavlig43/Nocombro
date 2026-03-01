package ru.pavlig43.storage.api.component

import ru.pavlig43.storage.api.column.StorageProductField
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object StorageFilterMatcher : FilterMatcher<StorageProductUi, StorageProductField>() {
    override fun matchesRules(
        item: StorageProductUi,
        column: StorageProductField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            StorageProductField.EXPAND -> true
            StorageProductField.NAME -> matchesTextField(item.name, stateAny)
            StorageProductField.BALANCE_BEFORE -> matchesIntField(item.balanceBeforeStart, stateAny)
            StorageProductField.INCOMING -> matchesIntField(item.incoming, stateAny)
            StorageProductField.OUTGOING -> matchesIntField(item.outgoing, stateAny)
            StorageProductField.BALANCE_END -> matchesIntField(item.balanceOnEnd, stateAny)
        }
    }
}
