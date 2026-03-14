package ru.pavlig43.expense.internal.model

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi

internal data class ExpenseEssentialsUi(
    val expenseType: ExpenseType? = null,
    val amount: Int = 0,
    val expenseDateTime: LocalDateTime = getCurrentLocalDateTime(),
    val comment: String = "",
    val id: Int = 0
) : ISingleLineTableUi

internal fun ExpenseEssentialsUi.toDto(): ExpenseBD {
    return ExpenseBD(
        transactionId = null,
        expenseType = expenseType ?: throw IllegalArgumentException("Expense type required"),
        amount = amount,
        expenseDateTime = expenseDateTime,
        comment = comment,
        id = id
    )
}

internal fun ExpenseBD.toUi(): ExpenseEssentialsUi {
    return ExpenseEssentialsUi(
        expenseType = expenseType,
        amount = amount,
        expenseDateTime = expenseDateTime,
        comment = comment,
        id = id
    )
}
