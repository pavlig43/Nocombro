package ru.pavlig43.expense.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository

internal fun createExpenseFormModule(dependencies: ExpenseFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transactionExecutor }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<UpdateSingleLineRepository<ExpenseBD>> { ExpenseRepository(get()) }
    }
)

private class ExpenseRepository(
    db: NocombroDatabase
) : UpdateSingleLineRepository<ExpenseBD> {

    private val dao = db.expenseDao
    override suspend fun getInit(id: Int): Result<ExpenseBD> {
        return runCatching {
            dao.getExpense(id)?: ExpenseBD(
                transactionId = null,
                expenseType = ExpenseType.TRANSPORT_GASOLINE,
                amount = 0,
                expenseDateTime = getCurrentLocalDateTime(),
                comment = "",
                id = 0
            )
        }
    }

    override suspend fun update(changeSet: ChangeSet<ExpenseBD>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            dao.updateExpense (changeSet.new)
        }
    }

}