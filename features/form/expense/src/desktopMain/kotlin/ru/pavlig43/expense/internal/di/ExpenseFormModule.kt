package ru.pavlig43.expense.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.UpsertListChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.dao.ExpenseDao
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.expense.internal.di.ExpenseStandaloneRepository
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository

internal fun createExpenseFormModule(dependencies: ExpenseFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transactionExecutor }
        single { dependencies.db.expenseDao }
        single<UpdateCollectionRepository<ExpenseBD, ru.pavlig43.database.data.expense.ExpenseBD>> {
            ExpenseStandaloneRepository(get())
        }
    }
)

internal class ExpenseStandaloneRepository(
    db: NocombroDatabase
) : UpdateCollectionRepository<ExpenseBD, ExpenseBD> {

    private val dao = db.expenseDao
    override suspend fun getInit(id: Int): Result<List<ExpenseBD>> {
        return runCatching {
            dao.getAll().filter { it.transactionId == null }
        }
    }

    override suspend fun update(changeSet: ChangeSet<List<ExpenseBD>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = { ids -> dao.deleteByIds(ids) },
            upsert = { expenses ->
                val expensesWithNullId = expenses.map { it.copy(transactionId = null) }
                dao.upsertAll(expensesWithNullId)
            }
        )
    }
}