package ru.pavlig43.expense.api

import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase

class ExpenseFormDependencies(
    val transactionExecutor: TransactionExecutor,
    val db: NocombroDatabase
)
