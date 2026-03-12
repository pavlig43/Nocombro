package ru.pavlig43.immutable.internal.component.items.productDeclaration

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState


internal object ProductDeclarationFilterMatcher : FilterMatcher<ProductDeclarationTableUi, ProductDeclarationField>() {

    override fun matchesRules(
        item: ProductDeclarationTableUi,
        column: ProductDeclarationField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {
                ProductDeclarationField.SELECTION -> true
                ProductDeclarationField.ID -> true
                ProductDeclarationField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
                ProductDeclarationField.DISPLAY_NAME -> matchesTextField(item.displayName,stateAny)
                ProductDeclarationField.IS_ACTUAL -> matchesBooleanField(item.isActual,stateAny)
            }
        return matches
    }
}