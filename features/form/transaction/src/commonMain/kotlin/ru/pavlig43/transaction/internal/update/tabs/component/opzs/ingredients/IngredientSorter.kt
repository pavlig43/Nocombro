package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object IngredientSorter : SortMatcher<IngredientUi, IngredientField> {

    override fun sort(
        items: List<IngredientUi>,
        sort: SortState<IngredientField>?
    ): List<IngredientUi> {
        if (sort == null) {
            return items
        }
        val sortedList = when (sort.column) {
            IngredientField.PRODUCT_NAME -> items.sortedBy { it.productName.lowercase() }
            IngredientField.VENDOR_NAME -> items.sortedBy { it.vendorName.lowercase() }
            IngredientField.COUNT -> items.sortedBy { it.balance }
            IngredientField.BATCH_NAME -> items.sortedBy { it.batchId }
            else -> items
        }
        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
