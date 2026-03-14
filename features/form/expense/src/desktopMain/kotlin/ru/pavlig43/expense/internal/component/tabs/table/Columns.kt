@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.expense.internal.component.tabs.table


import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.mutable.api.column.decimalColumn
import ru.pavlig43.mutable.api.column.writeDateTimeColumn
import ru.pavlig43.mutable.api.column.writeItemTypeColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

enum class ExpenseField {
    EXPENSE_TYPE,
    AMOUNT,
    EXPENSE_DATE_TIME,
    COMMENT
}
internal fun createExpenseColumns(
    openDateTimeDialog:()-> Unit,
    onChangeItem: ((ExpenseUi) -> ExpenseUi) -> Unit,
): ImmutableList<ColumnSpec<ExpenseUi, ExpenseField, Unit>> {
    val columns = editableTableColumns<ExpenseUi, ExpenseField, Unit> {


        writeItemTypeColumn(
            headerText = "Тип расхода",
            column = ExpenseField.EXPENSE_TYPE,
            valueOf = { it.expenseType },
            options = ExpenseType.entries,
            onTypeSelected = { item, type ->
                onChangeItem{it.copy(expenseType = type)}
            },
            filterType = TableFilterType.EnumTableFilter(
                options = ExpenseType.entries.toImmutableList(),
                getTitle = { it.displayName }
            ),
        )

        decimalColumn(
            key = ExpenseField.AMOUNT,
            getValue = { it.amount },
            headerText = "Сумма (₽)",
            updateItem = { item, amount ->
                onChangeItem{it.copy(amount = amount)}
            },
        )
        writeDateTimeColumn(
            headerText = "Время",
            column = ExpenseField.EXPENSE_DATE_TIME,
            valueOf = {it.expenseDateTime},
            onOpenDateTimeDialog = {openDateTimeDialog()},
        )


        writeTextColumn(
            headerText = "Комментарий",
            column = ExpenseField.COMMENT,
            valueOf = { it.comment },
            onChangeItem = { item, newValue ->
                onChangeItem{it.copy(comment = newValue)}
            },
            filterType = TableFilterType.TextTableFilter(),
        )
    }

    return columns
}
