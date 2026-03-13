package ru.pavlig43.immutable.internal.component.items.expense

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class ExpenseTableUi(
    val expenseType: ExpenseType = ExpenseType.STATIONERY,
    val amount: DecimalData = DecimalData(0, DecimalFormat.Decimal2),
    val expenseDateTime: LocalDateTime = LocalDateTime(2024, 1, 1, 0, 0),
    val comment: String = "",
    val transactionType: TransactionType? = null,
    override val composeId: Int = 0,
) : IMultiLineTableUi
