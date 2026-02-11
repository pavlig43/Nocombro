package ru.pavlig43.transaction.internal.update.tabs.component.expenses

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.transaction.expense.ExpenseType
import ru.pavlig43.tablecore.model.IMultiLineTableUi

internal data class ExpensesUi(
    override val composeId: Int,
    val id: Int,
    val transactionId: Int?,
    val expenseType: ExpenseType?,
    val amount: Int, // в копейках
    val expenseDateTime: LocalDateTime,
    val comment: String
) : IMultiLineTableUi

internal enum class ExpensesField {
    SELECTION,
    COMPOSE_ID,
    TRANSACTION_ID,
    EXPENSE_TYPE,
    AMOUNT,
    COMMENT
}
