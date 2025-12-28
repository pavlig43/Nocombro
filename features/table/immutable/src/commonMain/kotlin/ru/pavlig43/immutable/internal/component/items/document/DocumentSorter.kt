package ru.pavlig43.immutable.internal.component.items.document

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object DocumentSorter: SortMatcher<DocumentTableUi, DocumentField> {
    override fun sort(
        items: List<DocumentTableUi>,
        sort: SortState<DocumentField>?,
    ): List<DocumentTableUi>{
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                DocumentField.ID -> items.sortedBy { it.composeId }
                DocumentField.NAME -> items.sortedBy { it.displayName.lowercase() }
                DocumentField.TYPE -> items.sortedBy { it.type.displayName.lowercase() }
                DocumentField.CREATED_AT -> items.sortedBy { it.createdAt }
                DocumentField.COMMENT -> items.sortedBy { it.comment.lowercase() }
                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}