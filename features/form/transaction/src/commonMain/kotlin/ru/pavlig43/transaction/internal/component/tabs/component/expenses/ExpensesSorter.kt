package ru.pavlig43.transaction.internal.component.tabs.component.expenses

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object ExpensesSorter : SortMatcher<ExpensesUi, ExpensesField> {
    override fun sort(
        items: List<ExpensesUi>,
        sort: SortState<ExpensesField>?
    ): List<ExpensesUi> {
        if (sort == null) return items

        val sortedList = when (sort.column) {
            ExpensesField.EXPENSE_TYPE -> items.sortedBy { it.expenseType.displayName.lowercase() }
            ExpensesField.AMOUNT -> items.sortedBy { it.amount }
            ExpensesField.COMMENT -> items.sortedBy { it.comment.lowercase() }
            else -> items
        }

        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.reversed()
        } else {
            sortedList
        }
    }
}
