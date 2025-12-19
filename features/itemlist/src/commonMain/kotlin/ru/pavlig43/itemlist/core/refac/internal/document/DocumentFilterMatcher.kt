package ru.pavlig43.itemlist.core.refac.internal.document

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.core.refac.core.utils.DefaultFilterMatcher
import ru.pavlig43.itemlist.core.refac.core.utils.FilterMatcher
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import kotlin.collections.iterator


/**
 * Utility class for filtering Person objects based on filter constraints.
 */

internal object DocumentFilterMatcher: DefaultFilterMatcher<DocumentItemUi, DocumentField>() {
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

