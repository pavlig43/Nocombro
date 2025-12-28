package ru.pavlig43.immutable.internal.component.items.product

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object ProductFilterMatcher : FilterMatcher<ProductTableUi, ProductField>() {
    override fun matchesRules(
        item: ProductTableUi,
        column: ProductField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {
                ProductField.NAME -> matchesTextField(item.displayName, stateAny)
                ProductField.SELECTION -> true
                ProductField.ID -> matchesIntField(item.composeId, stateAny)
                ProductField.TYPE -> matchesTypeField(item.type, stateAny)
                ProductField.CREATED_AT -> matchesDateField(item.createdAt,stateAny)
                ProductField.COMMENT -> matchesTextField(item.comment, stateAny)
            }
        return  matches
    }

}