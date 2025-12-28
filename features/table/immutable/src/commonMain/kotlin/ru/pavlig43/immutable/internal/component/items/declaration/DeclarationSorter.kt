package ru.pavlig43.immutable.internal.component.items.declaration

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object DeclarationSorter: SortMatcher<DeclarationTableUi, DeclarationField> {
    override fun sort(
        items: List<DeclarationTableUi>,
        sort: SortState<DeclarationField>?,
    ): List<DeclarationTableUi>{
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                DeclarationField.ID -> items.sortedBy { it.composeId }
                DeclarationField.NAME -> items.sortedBy { it.displayName.lowercase() }
                DeclarationField.CREATED_AT -> items.sortedBy { it.createdAt }

                DeclarationField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
                DeclarationField.BEST_BEFORE -> items.sortedBy { it.bestBefore }
                                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}