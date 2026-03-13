@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.immutable.internal.component.items.expense

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.immutable.internal.column.idWithSelection
import ru.pavlig43.immutable.internal.column.readDateTimeColumn
import ru.pavlig43.immutable.internal.column.readDecimalColumn
import ru.pavlig43.immutable.internal.column.readEnumColumn
import ru.pavlig43.immutable.internal.column.readTextColumn
import ru.pavlig43.immutable.internal.component.ImmutableTableUiEvent
import ru.pavlig43.tablecore.model.TableData
import ua.wwind.table.ColumnSpec
import ua.wwind.table.ReadonlyColumnBuilder
import ua.wwind.table.ReadonlyTableColumnsBuilder
import ua.wwind.table.filter.data.TableFilterType
import ua.wwind.table.tableColumns

internal enum class ExpenseField {
    SELECTION,
    ID,
    EXPENSE_TYPE,
    AMOUNT,
    EXPENSE_DATE_TIME,
    COMMENT,
    TRANSACTION_TYPE
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
            filterType = TableFilterType.NumberTableFilter(delegate = TableFilterType.NumberTableFilter.IntDelegate)
        )

        readDateTimeColumn(
            headerText = "Дата и время",
            column = ExpenseField.EXPENSE_DATE_TIME,
            valueOf = { it.expenseDateTime },
            filterType = TableFilterType.DateTableFilter()
        )

        nullableTransactionTypeColumn(
            headerText = "Тип транзакции",
            column = ExpenseField.TRANSACTION_TYPE
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

private fun <T : Any, C, E> ReadonlyTableColumnsBuilder<T, C, E>.nullableTransactionTypeColumn(
    headerText: String,
    column: C
) {
    column(column, valueOf = { _: T -> }) {
        autoWidth(300.dp)
        header(headerText)
        align(Alignment.CenterStart)

        cell { item, _ ->
            val transactionType = (item as? ExpenseTableUi)?.transactionType
            Text(
                text = transactionType?.displayName ?: "",
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}
