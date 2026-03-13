package ru.pavlig43.immutable.internal.component.items.expense

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class ExpenseTableUi(
    val expenseType: ExpenseType,
    val amount: DecimalData,
    val expenseDateTime: LocalDateTime ,
    val comment: String ,
    val transactionType: TransactionType?,
    override val composeId: Int,
) : IMultiLineTableUi
