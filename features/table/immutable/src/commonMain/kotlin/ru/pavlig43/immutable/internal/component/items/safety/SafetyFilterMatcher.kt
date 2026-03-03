package ru.pavlig43.immutable.internal.component.items.safety

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object SafetyFilterMatcher : FilterMatcher<SafetyTableUi, SafetyField>() {
    override fun matchesRules(
        item: SafetyTableUi,
        column: SafetyField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            SafetyField.ID -> true
            SafetyField.PRODUCT_NAME -> matchesTextField(item.productName, stateAny)
            SafetyField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
            SafetyField.COUNT -> matchesTextField(item.count.toString(), stateAny)
            SafetyField.REORDER_POINT -> matchesTextField(item.reorderPoint.toString(), stateAny)
            SafetyField.ORDER_QUANTITY -> matchesTextField(item.orderQuantity.toString(), stateAny)
        }
    }
}
