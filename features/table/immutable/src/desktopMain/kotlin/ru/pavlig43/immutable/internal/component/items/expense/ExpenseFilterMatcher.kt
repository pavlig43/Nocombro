package ru.pavlig43.immutable.internal.component.items.expense

import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object ExpenseFilterMatcher : FilterMatcher<ExpenseTableUi, ExpenseField>() {
    override fun matchesRules(
        item: ExpenseTableUi,
        column: ExpenseField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            ExpenseField.SELECTION -> true
            ExpenseField.ID -> true
            ExpenseField.EXPENSE_TYPE -> matchesTypeField(item.expenseType, stateAny)
            ExpenseField.AMOUNT -> matchesIntField(item.amount.value, stateAny)
            ExpenseField.EXPENSE_DATE_TIME -> matchesDateTimeField(item.expenseDateTime, stateAny)
            ExpenseField.COMMENT -> matchesTextField(item.comment, stateAny)
            ExpenseField.TRANSACTION_TYPE -> {
                if (item.transactionType == null) {
                    when (stateAny.constraint) {
                        null -> true
                        else -> false
                    }
                } else {
                    matchesTypeField(item.transactionType, stateAny)
                }
            }
        }
    }
}
