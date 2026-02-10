package ru.pavlig43.transaction.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transaction.Transact
import ru.pavlig43.database.data.transaction.buy.BuyBDOut
import ru.pavlig43.database.data.transaction.expense.ExpenseBD
import ru.pavlig43.database.data.transaction.reminder.ReminderBD
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.update.data.UpdateCollectionRepository
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.dbTransaction }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateEssentialsRepository<Transact>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Transact>> { getUpdateRepository(get()) }
        single<UpdateCollectionRepository<BuyBDOut, BuyBDOut>>(UpdateCollectionRepositoryType.BUY.qualifier) {
            createUpdateBuyRepository(get())
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
): CreateEssentialsRepository<Transact> {
    val dao = db.transactionDao
    return CreateEssentialsRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Transact> {
    val dao = db.transactionDao
    return UpdateEssentialsRepository(
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
    db: NocombroDatabase
): UpdateCollectionRepository<BuyBDOut, BuyBDOut> {
    val buyDao = db.buyDao
//    val batchDao = db.ba
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


