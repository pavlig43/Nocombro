package ru.pavlig43.product.internal.update.tabs.composition

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object CompositionFilterMatcher : FilterMatcher<CompositionUi, CompositionField>() {
    override fun matchesRules(
        item: CompositionUi,
        column: CompositionField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {
                CompositionField.COMPOSE_ID -> true
                CompositionField.SELECTION -> true
                CompositionField.PRODUCT_NAME -> matchesTextField(item.productName, stateAny)
                CompositionField.COUNT -> matchesIntField(item.count, stateAny)
                CompositionField.PRODUCT_TYPE -> matchesTypeField(item.productType, stateAny)
            }
        return matches
    }
}
