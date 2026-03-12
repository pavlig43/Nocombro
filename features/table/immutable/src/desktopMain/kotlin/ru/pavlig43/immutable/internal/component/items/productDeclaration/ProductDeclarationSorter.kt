package ru.pavlig43.immutable.internal.component.items.productDeclaration

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState


internal object ProductDeclarationSorter :
    SortMatcher<ProductDeclarationTableUi, ProductDeclarationField> {
    override fun sort(
        items: List<ProductDeclarationTableUi>,
        sort: SortState<ProductDeclarationField>?,
    ): List<ProductDeclarationTableUi> {
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                ProductDeclarationField.ID -> items.sortedBy { it.composeId }
                ProductDeclarationField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
                ProductDeclarationField.DISPLAY_NAME -> items.sortedBy { it.displayName }
                ProductDeclarationField.IS_ACTUAL -> items.sortedBy { it.isActual }
                else -> items
            }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}