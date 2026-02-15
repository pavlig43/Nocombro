package ru.pavlig43.product.internal.update.tabs.declaration

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object ProductDeclarationSorter : SortMatcher<FlowProductDeclarationTableUi, ProductDeclarationField> {
    override fun sort(
        items: List<FlowProductDeclarationTableUi>,
        sort: SortState<ProductDeclarationField>?,
    ): List<FlowProductDeclarationTableUi> {
        if (sort == null) {
            return items
        }

        val sortedList =
            when (sort.column) {
                ProductDeclarationField.ID -> items.sortedBy { it.composeId }
                ProductDeclarationField.DECLARATION_NAME -> items.sortedBy { it.declarationName.lowercase() }
                ProductDeclarationField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
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
