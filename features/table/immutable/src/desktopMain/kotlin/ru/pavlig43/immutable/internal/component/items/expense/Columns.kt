@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.immutable.internal.component.items.expense

import androidx.compose.material3.Text
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.core.model.sumOfDecimal2
import ru.pavlig43.core.model.toStartDoubleFormat
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readBooleanColumn
import ru.pavlig43.immutable.internal.column.readDateTimeColumn
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readEnumColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ru.pavlig43.tablecore.utils.DataDecimalDelegate2
import ua.wwind.table.ColumnSpec
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class ExpenseField {
    SELECTION,
    ID,
    EXPENSE_TYPE,
    AMOUNT,
    EXPENSE_DATE_TIME,
    IS_MAIN,
    COMMENT,

}

internal fun createExpenseColumn(
    onEvent: (ImmutableTableUiEvent) -> Unit,
): ImmutableList<ColumnSpec<ExpenseTableUi, ExpenseField, TableData<ExpenseTableUi>>> {
    val columns = tableColumns<ExpenseTableUi, ExpenseField, TableData<ExpenseTableUi>> {
        idWithSelection(
            selectionKey = ExpenseField.SELECTION,
            idKey = ExpenseField.ID,
            onEvent = onEvent
        )

        readEnumColumn(
            headerText = "Тип расхода",
            column = ExpenseField.EXPENSE_TYPE,
            valueOf = { it.expenseType },
            filterType = TableFilterType.EnumTableFilter(
                ExpenseType.entries.toImmutableList(),
                getTitle = { it.displayName }
            ),
            getTitle = { it.displayName }
        )


        readDecimalColumn(
            headerText = "Сумма (₽)",
            column = ExpenseField.AMOUNT,
            valueOf = { it.amount },
            filterType = TableFilterType.NumberTableFilter(delegate = DataDecimalDelegate2),
            footerContent = { tableData ->
                val sum = tableData.displayedItems.sumOfDecimal2 { it.amount }
                Text(sum.toStartDoubleFormat())
            }
        )

        readDateTimeColumn(
            headerText = "Дата и время",
            column = ExpenseField.EXPENSE_DATE_TIME,
            valueOf = { it.expenseDateTime },
            filterType = TableFilterType.DateTableFilter()
        )
        readBooleanColumn(
            headerText = "Общие",
            column = ExpenseField.IS_MAIN,
            valueOf = { it.isMain },
            filterType = TableFilterType.BooleanTableFilter(),
        )


        readTextColumn(
            headerText = "Комментарий",
            column = ExpenseField.COMMENT,
            valueOf = { it.comment },
            filterType = TableFilterType.TextTableFilter(),
            isSortable = false
        )
    }
    return columns
}

