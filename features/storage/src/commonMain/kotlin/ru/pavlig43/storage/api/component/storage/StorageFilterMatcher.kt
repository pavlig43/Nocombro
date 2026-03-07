package ru.pavlig43.storage.api.component.storage

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
            StorageProductField.NAME -> matchesTextField(item.productName, stateAny)
            else -> true
        }
    }
}
