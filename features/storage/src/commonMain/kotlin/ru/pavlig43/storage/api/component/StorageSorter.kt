package ru.pavlig43.storage.api.component

import ru.pavlig43.storage.api.column.StorageProductField
import ru.pavlig43.storage.internal.model.StorageProductUi
import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object StorageSorter : SortMatcher<StorageProductUi, StorageProductField> {
    override fun sort(
        items: List<StorageProductUi>,
        sort: SortState<StorageProductField>?,
    ): List<StorageProductUi> {
        if (sort == null) {
            return items
        }

        val sortedList = when (sort.column) {
            StorageProductField.EXPAND -> items.sortedBy { it.productId }
            StorageProductField.NAME -> items.sortedBy { it.name.lowercase() }
            StorageProductField.BALANCE_BEFORE -> items.sortedBy { it.balanceBeforeStart }
            StorageProductField.INCOMING -> items.sortedBy { it.incoming }
            StorageProductField.OUTGOING -> items.sortedBy { it.outgoing }
            StorageProductField.BALANCE_END -> items.sortedBy { it.balanceOnEnd }
        }

        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
