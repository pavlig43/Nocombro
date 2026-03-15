package ru.pavlig43.immutable.internal.component.items.expense

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class ExpenseTableUi(
    val expenseType: ExpenseType,
    val amount: DecimalData2,
    val expenseDateTime: LocalDateTime ,
    val comment: String ,
    val transactionId: Int?,
    override val composeId: Int,
) : IMultiLineTableUi{
    val isMain = transactionId == null
}
