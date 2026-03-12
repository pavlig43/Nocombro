package ru.pavlig43.transaction.internal.update.tabs.component.expenses

import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState

internal object ExpensesFilterMatcher : FilterMatcher<ExpensesUi, ExpensesField>() {
    override fun matchesRules(
        item: ExpensesUi,
        column: ExpensesField,
        stateAny: TableFilterState<*>
    ): Boolean {
        return when (column) {
            ExpensesField.SELECTION -> true
            ExpensesField.COMPOSE_ID -> true
            ExpensesField.TRANSACTION_ID -> true
            ExpensesField.EXPENSE_TYPE -> matchesTypeField(item.expenseType?.displayName, stateAny)
            ExpensesField.AMOUNT -> true
            ExpensesField.COMMENT -> matchesTextField(item.comment, stateAny)
        }
    }
}
