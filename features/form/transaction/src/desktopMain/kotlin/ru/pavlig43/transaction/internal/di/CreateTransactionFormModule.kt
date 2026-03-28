@file:Suppress("MagicNumber")

package ru.pavlig43.transaction.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.UpsertListChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.BuyBDOut
import ru.pavlig43.database.data.transact.ingredient.IngredientBD
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.transact.sale.SaleBDOut
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients.FillIngredientsRepository
import kotlin.math.roundToLong

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.dbTransaction }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateSingleItemRepository<Transact>> { TransactionCreateRepository(get()) }
        single<UpdateSingleLineRepository<Transact>>(UpdateSingleLineRepositoryType.TRANSACTION.qualifier) {
            TransactionUpdateRepository(
                get()
            )
        }
        single<UpdateCollectionRepository<BuyBDOut, BuyBDOut>>(UpdateCollectionRepositoryType.BUY.qualifier) {
            BuyCollectionRepository(get())
        }
        single<UpdateCollectionRepository<ReminderBD, ReminderBD>>(UpdateCollectionRepositoryType.REMINDERS.qualifier) {
            RemindersCollectionRepository(get())
        }
        single<UpdateCollectionRepository<ExpenseBD, ExpenseBD>>(UpdateCollectionRepositoryType.EXPENSES.qualifier) {
            ExpensesCollectionRepository(get())
        }
        single<UpdateCollectionRepository<IngredientBD, IngredientBD>>(
            UpdateCollectionRepositoryType.INGREDIENTS.qualifier
        ) {
            IngredientsCollectionRepository(get())
        }
        single<UpdateCollectionRepository<SaleBDOut, SaleBDOut>>(UpdateCollectionRepositoryType.SALE.qualifier) {
            SaleCollectionRepository(get())
        }
        single<UpdateSingleLineRepository<PfBD>>(UpdateSingleLineRepositoryType.PF.qualifier) {
            PfUpdateRepository(
                get()
            )
        }
        singleOf(::FillIngredientsRepository)
        singleOf(::BatchCostRepository)
    }
)

internal enum class UpdateCollectionRepositoryType {

    BUY,
    REMINDERS,
    EXPENSES,
    INGREDIENTS,
    SALE

}

internal enum class UpdateSingleLineRepositoryType {
    TRANSACTION,
    PF
}

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
    db: NocombroDatabase
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

    override suspend fun update(changeSet: ChangeSet<List<BuyBDOut>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = ::deleteBuysWithMovement,
            upsert = ::upsertAllEntity
        )
    }

    private suspend fun deleteBuysWithMovement(buyIds: List<Int>) {
        val movementIds = buyDao.getMovementIdsByBuyIds(buyIds)
        buyDao.deleteByIds(buyIds)
        movementDao.deleteByIds(movementIds)
    }

    private suspend fun upsertAllEntity(buys: List<BuyBDOut>) {
        buys.forEach { buy ->
            val batch = BatchBD(
                id = buy.batchId,
                productId = buy.productId,
                dateBorn = buy.dateBorn,
                declarationId = buy.declarationId
            )
            val batchId = if (batch.id == 0) {
                batchDao.createBatch(batch).toInt()
            } else {
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
            val movementId = if (movement.id == 0) {
                movementDao.createMovement(movement).toInt()
            } else {
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


private class RemindersCollectionRepository(
    db: NocombroDatabase
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
    db: NocombroDatabase
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


private class PfUpdateRepository(
    db: NocombroDatabase
) : UpdateSingleLineRepository<PfBD> {

    private val pfDao = db.pfDao
    private val batchDao = db.batchDao
    private val movementDao = db.batchMovementDao

    override suspend fun getInit(id: Int): Result<PfBD> {
        return runCatching {
            pfDao.getPf(id) ?: PfBD()
        }
    }

    override suspend fun update(changeSet: ChangeSet<PfBD>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)

        return runCatching {
            val pf = changeSet.new

            // 1. Создаём/обновляем Batch
            val batch = BatchBD(
                id = pf.batchId,
                productId = pf.productId,
                dateBorn = pf.dateBorn,
                declarationId = pf.declarationId
            )
            val batchId = if (batch.id == 0) {
                batchDao.createBatch(batch).toInt()
            } else {
                batchDao.updateBatch(batch)
                batch.id
            }

            // 2. Создаём/обновляем BatchMovement INCOMING
            val movement = BatchMovement(
                batchId = batchId,
                movementType = MovementType.INCOMING,
                count = pf.count,
                transactionId = pf.transactionId,
                id = pf.movementId
            )
            if (movement.id == 0) {
                movementDao.createMovement(movement)
            } else {
                movementDao.upsertMovement(movement)
            }
        }
    }
}


private class IngredientsCollectionRepository(
    db: NocombroDatabase
) : UpdateCollectionRepository<IngredientBD, IngredientBD> {

    private val ingredientDao = db.ingredientDao
    private val movementDao = db.batchMovementDao

    override suspend fun getInit(id: Int): Result<List<IngredientBD>> {
        return runCatching {
            ingredientDao.getByTransactionId(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<List<IngredientBD>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = movementDao::deleteByIds,
            upsert = ::upsertIngredients
        )
    }

    private suspend fun upsertIngredients(ingredients: List<IngredientBD>) {
        ingredients.map { ingredient ->
            BatchMovement(
                batchId = ingredient.batchId,
                movementType = MovementType.OUTGOING,
                count = ingredient.count,
                transactionId = ingredient.transactionId,
                id = ingredient.movementId
            )
        }.also {
            movementDao.upsertMovements(it)
        }
    }
}


private class SaleCollectionRepository(
    db: NocombroDatabase
) : UpdateCollectionRepository<SaleBDOut, SaleBDOut> {

    private val saleDao = db.saleDao
    private val movementDao = db.batchMovementDao

    override suspend fun getInit(id: Int): Result<List<SaleBDOut>> {
        return runCatching {
            saleDao.getSalesWithDetails(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<List<SaleBDOut>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = ::deleteSalesWithMovement,
            upsert = ::upsertAllEntity
        )
    }

    private suspend fun deleteSalesWithMovement(saleIds: List<Int>) {
        val movementIds = saleDao.getMovementIdsBySaleIds(saleIds)
        saleDao.deleteByIds(saleIds)
        movementDao.deleteByIds(movementIds)
    }

    private suspend fun upsertAllEntity(sales: List<SaleBDOut>) {
        sales.forEach { sale ->


            val movement = BatchMovement(
                batchId = sale.batchId,
                movementType = MovementType.OUTGOING,
                count = sale.count,
                transactionId = sale.transactionId,
                id = sale.movementId
            )
            val movementId = if (movement.id == 0) {
                movementDao.createMovement(movement).toInt()
            } else {
                movementDao.upsertMovement(movement)
                movement.id
            }
            val saleBdIn = SaleBDIn(
                transactionId = sale.transactionId,
                movementId = movementId,
                price = sale.price,
                comment = sale.comment,
                clientId = sale.clientId,
                id = sale.id
            )
            saleDao.upsertSaleBd(saleBdIn)
        }
    }
}

internal class BatchCostRepository(
    db: NocombroDatabase
) {
    private val batchCostDao = db.batchCostDao
    private val transactionDao = db.transactionDao

    private val buyDao = db.buyDao

    private val batchMovementDao = db.batchMovementDao

    private val expenseDao = db.expenseDao
    suspend fun updateBatchCost(transactionId: Int) {
        val transaction = transactionDao.getTransaction(transactionId)
        when (transaction.transactionType) {
            TransactionType.BUY -> upsertBatchCostFromBuy(transactionId)
            TransactionType.OPZS -> upsertBatchCostFromOpzs(transactionId)
            else -> return
        }
    }

    private suspend fun upsertBatchCostFromBuy(transactionId: Int) {
        val buys = buyDao.getBuysWithDetails(transactionId)
        val expenses = expenseDao.getByTransactionId(transactionId)
        val transactionExpenses = expenses.sumOf { it.amount }

        val quantityAmount = buys.sumOf { it.count }
        val expenseOnOneKg = (transactionExpenses / quantityAmount.toDouble()) * 1000
        val batchCostPriceEntities = buys.map { buyBDOut ->
            BatchCostPriceEntity(
                batchId = buyBDOut.batchId,
                costPricePerUnit = (buyBDOut.price + expenseOnOneKg).roundToLong()
            )
        }
        batchCostDao.upsert(batchCostPriceEntities)
    }

    private suspend fun upsertBatchCostFromOpzs(transactionId: Int) {
        val (pf, ingredients) = batchMovementDao.getByTransactionId(transactionId)
            .partition { it.movement.movementType == MovementType.INCOMING }
        if (pf.isEmpty()) return
        val pfMovement = pf.first()
        val ingredientsCostMap = ingredients
            .map { it.movement.batchId }
            .let { batchCostDao.getBatchesCostPriceByIds(it) }
            .associateBy { it.batchId }

        val totalCost = ingredients.sumOf { ingredient ->
            val costPerKg = ingredientsCostMap[ingredient.movement.batchId]?.costPricePerUnit ?: 0L
            (costPerKg * ingredient.movement.count) / 1000.0
        }
        val costPricePerKg = if (pfMovement.movement.count > 0) {
            (totalCost / pfMovement.movement.count.toDouble()) * 1000
        } else 0.0
        batchCostDao.upsert(
            listOf(
                BatchCostPriceEntity(pfMovement.movement.batchId, costPricePerKg.roundToLong())
            )
        )
    }
}

