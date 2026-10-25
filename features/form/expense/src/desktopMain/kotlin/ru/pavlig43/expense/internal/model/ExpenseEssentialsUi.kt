package ru.pavlig43.expense.internal.model

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import ru.pavlig43.mutable.api.input.decimalValue
import ru.pavlig43.mutable.api.input.toDecimalInputText
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.getCurrentLocalDateTime

internal data class ExpenseEssentialsUi(
    val expenseType: ExpenseType? = null,
    val amountState: TextFieldState = TextFieldState(),
    val expenseDateTime: LocalDateTime = getCurrentLocalDateTime(),
    val commentState: TextFieldState = TextFieldState(),
    val id: Int = 0,
    val syncId: String = defaultSyncId(),
    val updatedAt: LocalDateTime = defaultUpdatedAt(),
    val deletedAt: LocalDateTime? = null,
) : SingleLineInputState {
    val comment: String get() = commentState.text.toString()
    val amount: DecimalData2 get() = DecimalData2(amountState.decimalValue(2) ?: 0L)
    override val textFields get() = listOf(commentState, amountState)
    override val inputErrors get() = if (
        amountState.decimalValue(2) == null
    ) listOf("Некорректное число") else emptyList()
}

internal fun ExpenseEssentialsUi.toDto(): ExpenseBD {
    return ExpenseBD(
        transactionId = null,
        expenseType = expenseType ?: throw IllegalArgumentException("Expense type required"),
        amount = amount.value,
        expenseDateTime = expenseDateTime,
        comment = comment,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}

internal fun ExpenseBD.toUi(): ExpenseEssentialsUi {
    return ExpenseEssentialsUi(
        expenseType = expenseType,
        amountState = TextFieldState((DecimalData2(amount)).toDecimalInputText()),
        expenseDateTime = expenseDateTime,
        commentState = TextFieldState(comment),
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
