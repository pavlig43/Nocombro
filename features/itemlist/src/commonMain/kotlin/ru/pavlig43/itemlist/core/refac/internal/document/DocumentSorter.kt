package ru.pavlig43.itemlist.core.refac.internal.document

import ru.pavlig43.itemlist.core.refac.core.utils.SortMatcher
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object DocumentSorter: SortMatcher<DocumentItemUi, DocumentField> {
    override fun sort(
        items: List<DocumentItemUi>,
        sort: SortState<DocumentField>?,
    ): List<DocumentItemUi>{
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                DocumentField.ID -> items.sortedBy { it.id }
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