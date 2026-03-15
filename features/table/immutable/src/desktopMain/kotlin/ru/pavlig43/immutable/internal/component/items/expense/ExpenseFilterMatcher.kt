package ru.pavlig43.immutable.internal.component.items.expense

import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.tablecore.utils.FilterMatcher
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType.NumberTableFilter.NumberFilterDelegate

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
            ExpenseField.AMOUNT -> matchesDecimalField(item.amount, stateAny)
            ExpenseField.EXPENSE_DATE_TIME -> matchesDateTimeField(item.expenseDateTime, stateAny)
            ExpenseField.COMMENT -> matchesTextField(item.comment, stateAny)
            ExpenseField.IS_MAIN -> matchesBooleanField(item.isMain,stateAny)
        }
    }
}

