package ru.pavlig43.expense.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction
import ru.pavlig43.datetime.getCurrentLocalDateTime
import ru.pavlig43.expense.api.ExpenseFormDependencies
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncUpdateSingleLineRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository

internal fun createExpenseFormModule(dependencies: ExpenseFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transactionExecutor }
        single<FilesDependencies> { dependencies.filesDependencies }
        single { SyncQueueRepository(get<NocombroDatabase>().syncDao) }
        single<UpdateSingleLineRepository<ExpenseBD>> { ExpenseRepository(get(), get()) }
        single<CreateSingleItemRepository<ExpenseBD>> { CreateExpenseRepository(get(), get()) }
    }
)

private class ExpenseRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateSingleLineRepository<ExpenseBD>(
    tableName = EXPENSE_TABLE_NAME,
    entitySyncKeyOf = ExpenseBD::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

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

    override fun prepareForUpdate(item: ExpenseBD): ExpenseBD = item.copy(updatedAt = defaultUpdatedAt())

    override suspend fun updateInDb(item: ExpenseBD) {
        dao.updateExpense(item)
    }

}

private class CreateExpenseRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncCreateSingleItemRepository<ExpenseBD>(
    tableName = EXPENSE_TABLE_NAME,
    entitySyncKeyOf = ExpenseBD::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.expenseDao

    override suspend fun createInDb(item: ExpenseBD): Int {
        return dao.insertExpense(item).toInt()
    }
}
