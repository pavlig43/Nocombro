package ru.pavlig43.immutable.internal.component.items.expense

import ru.pavlig43.tablecore.utils.SortMatcher
import ua.wwind.table.data.SortOrder
import ua.wwind.table.state.SortState

internal object ExpenseSorter : SortMatcher<ExpenseTableUi, ExpenseField> {
    override fun sort(
        items: List<ExpenseTableUi>,
        sort: SortState<ExpenseField>?,
    ): List<ExpenseTableUi> {
        if (sort == null) {
            return items.sortedByDescending { it.expenseDateTime }
        }

        val sortedList = when (sort.column) {
            ExpenseField.EXPENSE_TYPE -> items.sortedBy { it.expenseType }
            ExpenseField.AMOUNT -> items.sortedBy { it.amount.value }
            ExpenseField.EXPENSE_DATE_TIME -> items.sortedBy { it.expenseDateTime }
            ExpenseField.COMMENT -> items.sortedBy { it.comment }
            ExpenseField.TRANSACTION_TYPE -> items.sortedBy { it.transactionType }
            else -> items
        }

        return if (sort.order == SortOrder.DESCENDING) {
            sortedList.asReversed()
        } else {
            sortedList
        }
    }
}
