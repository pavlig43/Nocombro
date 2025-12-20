package ru.pavlig43.itemlist.internal.component.items.document

import ru.pavlig43.itemlist.internal.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState


/**
 * Utility class for filtering Person objects based on filter constraints.
 */

internal object DocumentFilterMatcher: FilterMatcher<DocumentItemUi, DocumentField>() {
    override fun matchesRules(
        item: DocumentItemUi,
        column: DocumentField,
        stateAny: TableFilterState<*>
    ): Boolean {
        val matches =
            when (column) {
                DocumentField.NAME -> matchesTextField(item.displayName, stateAny)
                DocumentField.SELECTION -> true
                DocumentField.ID -> matchesIntField(item.id, stateAny)
                DocumentField.TYPE -> matchesTypeField(item.type, stateAny)
                DocumentField.CREATED_AT -> matchesDateField(item.createdAt,stateAny)
                DocumentField.COMMENT -> matchesTextField(item.comment, stateAny)
            }
        return  matches
    }

    }

