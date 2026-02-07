package ru.pavlig43.transaction.internal.component.tabs.component.expenses

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.emptyLocalDateTime
import ru.pavlig43.database.data.transaction.expense.ExpenseTypeEnum
import ru.pavlig43.tablecore.model.ITableUi

internal data class ExpensesUi(
    override val composeId: Int,
    val id: Int,
    val transactionId: Int?,
    val expenseType: ExpenseTypeEnum,
    val amount: Int, // в копейках
    val expenseDateTime: LocalDateTime,
    val comment: String
) : ITableUi

internal enum class ExpensesField {
    SELECTION,
    COMPOSE_ID,
    TRANSACTION_ID,
    EXPENSE_TYPE,
    AMOUNT,
    COMMENT
}
