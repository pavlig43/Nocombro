package ru.pavlig43.itemlist.internal.component.items.declaration

import ru.pavlig43.itemlist.internal.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object DeclarationFilterMatcher: FilterMatcher<DeclarationItemUi, DeclarationField>() {


    override fun matchesRules(
        item: DeclarationItemUi,
        column: DeclarationField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {
                DeclarationField.SELECTION -> true
                DeclarationField.ID -> true
                DeclarationField.NAME -> matchesTextField(item.displayName, stateAny)
                DeclarationField.VENDOR_NAME -> matchesTextField(item.vendorName, stateAny)
                DeclarationField.BEST_BEFORE -> matchesDateField(item.bestBefore, stateAny)
                DeclarationField.CREATED_AT -> matchesDateField(item.createdAt, stateAny)
            }
        return matches
            }
    }

