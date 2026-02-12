package ru.pavlig43.transaction.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.BuyBDOut
import ru.pavlig43.database.data.transact.expense.ExpenseBD
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.transaction.api.TransactionFormDependencies

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.dbTransaction }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateSingleItemRepository<Transact>> { getCreateRepository(get()) }
        single<UpdateSingleLineRepository<Transact>> { getUpdateRepository(get()) }
        single<UpdateCollectionRepository<BuyBDOut, BuyBDIn>>(UpdateCollectionRepositoryType.BUY.qualifier) {
            createUpdateBuyRepository()
        }
        single<UpdateCollectionRepository<ReminderBD, ReminderBD>>(UpdateCollectionRepositoryType.REMINDERS.qualifier) {
            createUpdateRemindersRepository(get())
        }
        single<UpdateCollectionRepository<ExpenseBD, ExpenseBD>>(UpdateCollectionRepositoryType.EXPENSES.qualifier) {
            createUpdateExpensesRepository(get())
        }


    }

)


private fun getCreateRepository(
    db: NocombroDatabase
): CreateSingleItemRepository<Transact> {
    val dao = db.transactionDao
    return CreateSingleItemRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateSingleLineRepository<Transact> {
    val dao = db.transactionDao
    return UpdateSingleLineRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getTransaction,
        updateItem = dao::updateTransaction
    )
}

internal enum class UpdateCollectionRepositoryType {

    BUY,
    REMINDERS,
    EXPENSES

}

private fun createUpdateBuyRepository(
//    db: NocombroDatabase
): UpdateCollectionRepository<BuyBDOut, BuyBDIn> {
    return UpdateCollectionRepository(
        loadCollection = { emptyList() },
        deleteCollection = {},
        upsertCollection = {}
    )
}

private fun createUpdateRemindersRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ReminderBD, ReminderBD> {
    val dao = db.reminderDao
    return UpdateCollectionRepository(
        loadCollection = { transactionId ->
            dao.getByTransactionId(transactionId)
        },
        deleteCollection = { ids ->
            dao.deleteByIds(ids)
        },
        upsertCollection = { reminders ->
            dao.upsertAll(reminders)
        }
    )
}

private fun createUpdateExpensesRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ExpenseBD, ExpenseBD> {
    val dao = db.expenseDao
    return UpdateCollectionRepository(
        loadCollection = { transactionId ->
            dao.getByTransactionId(transactionId)
        },
        deleteCollection = { ids ->
            dao.deleteByIds(ids)
        },
        upsertCollection = { expenses ->
            dao.upsertAll(expenses)
        }
    )
}


