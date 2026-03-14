package ru.pavlig43.expense.internal.component.tabs.table

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class ExpenseUi(
    val id: Int = 0,
    val expenseType: ExpenseType? = null,
    val amount: DecimalData = DecimalData(0, DecimalFormat.Decimal2),
    val expenseDateTime: LocalDateTime = getCurrentLocalDateTime(),
    val comment: String = ""
) : ISingleLineTableUi