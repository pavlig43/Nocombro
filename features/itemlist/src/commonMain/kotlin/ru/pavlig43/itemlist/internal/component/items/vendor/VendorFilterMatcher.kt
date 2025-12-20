package ru.pavlig43.itemlist.internal.component.items.vendor

import ru.pavlig43.itemlist.internal.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object VendorFilterMatcher : FilterMatcher<VendorItemUi, VendorField>() {
    override fun matchesRules(
        item: VendorItemUi,
        column: VendorField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {
                VendorField.SELECTION -> true
                VendorField.ID -> true
                VendorField.NAME -> matchesTextField(item.displayName, stateAny)
                VendorField.COMMENT -> matchesTextField(item.comment, stateAny)


            }
        return  matches
    }

}