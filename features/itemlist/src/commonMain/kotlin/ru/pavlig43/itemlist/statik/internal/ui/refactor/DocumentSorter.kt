package ru.pavlig43.itemlist.statik.internal.ui.refactor

import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

object DocumentSorter {
    fun sortDocuments(
        documents: List<DocumentItemUi>,
        sort: SortState<DocumentField>?,
    ): List<DocumentItemUi>{
        if (sort == null) {
            return documents
        }

        val sortedList =
            when (sort.column) {
                DocumentField.ID -> documents.sortedBy { it.id }
                DocumentField.NAME -> documents.sortedBy { it.displayName.lowercase() }
                DocumentField.TYPE -> documents.sortedBy { it.type.displayName.lowercase() }
                DocumentField.CREATED_AT -> documents.sortedBy { it.createdAt }
                DocumentField.COMMENT -> documents.sortedBy { it.comment.lowercase() }
                else -> documents
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
