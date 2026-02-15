package ru.pavlig43.product.internal.update.tabs.declaration

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object ProductDeclarationFilterMatcher : FilterMatcher<FlowProductDeclarationTableUi, ProductDeclarationField>() {

    override fun matchesRules(
        item: FlowProductDeclarationTableUi,
        column: ProductDeclarationField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            ProductDeclarationField.SELECTION -> true
            ProductDeclarationField.ID -> true
            ProductDeclarationField.DECLARATION_NAME -> matchesTextField(item.declarationName, stateAny)
            ProductDeclarationField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
            ProductDeclarationField.IS_ACTUAL -> matchesBooleanField(item.isActual, stateAny)
        }
    }
}
