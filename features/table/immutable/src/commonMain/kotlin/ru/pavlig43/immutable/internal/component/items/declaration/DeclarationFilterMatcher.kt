package ru.pavlig43.immutable.internal.component.items.declaration

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object DeclarationFilterMatcher: FilterMatcher<DeclarationTableUi, DeclarationField>() {


    override fun matchesRules(
        item: DeclarationTableUi,
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

