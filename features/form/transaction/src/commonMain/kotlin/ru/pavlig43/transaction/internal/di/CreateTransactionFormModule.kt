package ru.pavlig43.transaction.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.UpsertListChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.buy.BuyBDOut
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.transaction.api.TransactionFormDependencies

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.dbTransaction }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateSingleItemRepository<Transact>> { TransactionCreateRepository(get()) }
        single<UpdateSingleLineRepository<Transact>> { TransactionUpdateRepository(get()) }
        single<UpdateCollectionRepository<BuyBDOut, BuyBDOut>>(UpdateCollectionRepositoryType.BUY.qualifier) {
            BuyCollectionRepository(get())
        }
        single<UpdateCollectionRepository<ReminderBD, ReminderBD>>(UpdateCollectionRepositoryType.REMINDERS.qualifier) {
            RemindersCollectionRepository(get())
        }
        single<UpdateCollectionRepository<ExpenseBD, ExpenseBD>>(UpdateCollectionRepositoryType.EXPENSES.qualifier) {
            ExpensesCollectionRepository(get())
        }


    }

)


private class TransactionCreateRepository(db: NocombroDatabase) :
    CreateSingleItemRepository<Transact> {
    private val dao = db.transactionDao

    override suspend fun createEssential(item: Transact): Result<Int> {
        return runCatching {
            dao.isCanSave(item).getOrThrow()
            dao.create(item).toInt()
        }
    }
}

private class TransactionUpdateRepository(
    private val db: NocombroDatabase
) : UpdateSingleLineRepository<Transact> {

    private val dao = db.transactionDao

    override suspend fun getInit(id: Int): Result<Transact> {
        return runCatching {
            dao.getTransaction(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<Transact>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            dao.isCanSave(changeSet.new).getOrThrow()
            dao.updateTransaction(changeSet.new)
        }
    }
}


private class BuyCollectionRepository(
    db: NocombroDatabase
) : UpdateCollectionRepository<BuyBDOut, BuyBDOut> {

    private val buyDao = db.buyDao
    private val batchDao = db.batchDao
    private val movementDao = db.batchMovementDao

    override suspend fun getInit(id: Int): Result<List<BuyBDOut>> {
        return runCatching {
            buyDao.getBuysWithDetails(id)
        }
    }
    private suspend fun getBuyBDOut(transactionId: Int){
        val buy = buyDao.getBuyBD(transactionId)
        val movement = movementDao.getByTransactionId(transactionId)

    }

    override suspend fun update(changeSet: ChangeSet<List<BuyBDOut>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = { ids -> buyDao.deleteByIds(ids) },
            upsert = ::upsertAllEntity
        )
    }

    suspend fun upsertAllEntity(buys: List<BuyBDOut>) {
        buys.forEach {buy->
            val batch = BatchBD(
                id = buy.batchId,
                productId = buy.productId,
                dateBorn = buy.dateBorn,
                declarationId = buy.declarationId
            )
            val batchId = if (batch.id == 0){
                batchDao.createBatch(batch).toInt()
            }
            else{
                batchDao.updateBatch(batch)
                batch.id
            }
            val movement = BatchMovement(
                batchId = batchId,
                movementType = MovementType.INCOMING,
                count = buy.count,
                transactionId = buy.transactionId,
                id = buy.movementId
            )
            val movementId = if (movement.id == 0){
                movementDao.createMovement(movement).toInt()
            }
            else{
                movementDao.upsertMovement(movement)
                movement.id
            }
            val buyBdIn = BuyBDIn(
                transactionId = buy.transactionId,
                movementId = movementId,
                price = buy.price,
                comment = buy.comment,
                id = buy.id
            )
            buyDao.upsertBuyBd(buyBdIn)
        }
    }
}

internal enum class UpdateCollectionRepositoryType {

    BUY,
    REMINDERS,
    EXPENSES

}


private class RemindersCollectionRepository(
    private val db: NocombroDatabase
) : UpdateCollectionRepository<ReminderBD, ReminderBD> {

    private val dao = db.reminderDao

    override suspend fun getInit(id: Int): Result<List<ReminderBD>> {
        return runCatching {
            dao.getByTransactionId(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<List<ReminderBD>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = { ids -> dao.deleteByIds(ids) },
            upsert = { reminders -> dao.upsertAll(reminders) }
        )
    }
}


private class ExpensesCollectionRepository(
    private val db: NocombroDatabase
) : UpdateCollectionRepository<ExpenseBD, ExpenseBD> {

    private val dao = db.expenseDao

    override suspend fun getInit(id: Int): Result<List<ExpenseBD>> {
        return runCatching {
            dao.getByTransactionId(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<List<ExpenseBD>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = { ids -> dao.deleteByIds(ids) },
            upsert = { expenses -> dao.upsertAll(expenses) }
        )
    }
}



