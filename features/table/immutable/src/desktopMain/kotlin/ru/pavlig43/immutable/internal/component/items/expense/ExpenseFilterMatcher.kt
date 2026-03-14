package ru.pavlig43.immutable.internal.component.items.expense

import ru.pavlig43.core.model.DecimalData
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
            ExpenseField.AMOUNT -> matchesIntField(item.amount.value, stateAny)
            ExpenseField.EXPENSE_DATE_TIME -> matchesDateTimeField(item.expenseDateTime, stateAny)
            ExpenseField.COMMENT -> matchesTextField(item.comment, stateAny)
            ExpenseField.IS_MAIN -> matchesBooleanField(item.isMain,stateAny)
        }
    }
}
object DataDecimalDelegate:NumberFilterDelegate<DecimalData> {
    override fun compare(
        a: DecimalData,
        b: DecimalData
    ): Boolean {
        TODO("Not yet implemented")
    }

    override fun format(value: DecimalData): String {
        TODO("Not yet implemented")
    }

    override fun fromSliderValue(value: Float): DecimalData {
        TODO("Not yet implemented")
    }

    override fun parse(input: String): DecimalData? {
        TODO("Not yet implemented")
    }

    override fun toSliderValue(value: DecimalData): Float {
        TODO("Not yet implemented")
    }

    override val default: DecimalData
        get() = TODO("Not yet implemented")
    override val regex: Regex
        get() = TODO("Not yet implemented")
}
