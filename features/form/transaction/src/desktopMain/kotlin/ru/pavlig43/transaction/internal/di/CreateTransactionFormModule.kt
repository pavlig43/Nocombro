@file:Suppress("MagicNumber")

package ru.pavlig43.transaction.internal.di

import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.UpsertListChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.data.batch.BATCH_MOVEMENT_TABLE_NAME
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.expense.EXPENSE_TABLE_NAME
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TRANSACTION_TABLE_NAME
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.buy.BuyBDIn
import ru.pavlig43.database.data.transact.buy.BuyBDOut
import ru.pavlig43.database.data.transact.ingredient.IngredientBD
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.database.data.transact.reminder.REMINDER_TABLE_NAME
import ru.pavlig43.database.data.transact.reminder.ReminderBD
import ru.pavlig43.database.data.transact.sale.SaleBDIn
import ru.pavlig43.database.data.transact.sale.SaleBDOut
import ru.pavlig43.database.inTransaction
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.SyncUpdateCollectionRepository
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncUpdateSingleLineRepository
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
        single<CreateSingleItemRepository<Transact>> { TransactionCreateRepository(get(), get()) }
        single<UpdateSingleLineRepository<Transact>>(UpdateSingleLineRepositoryType.TRANSACTION.qualifier) {
            TransactionUpdateRepository(
                get(),
                get(),
            )
        }
        single<UpdateCollectionRepository<BuyBDOut, BuyBDOut>>(UpdateCollectionRepositoryType.BUY.qualifier) {
            BuyCollectionRepository(get())
        }
        single<UpdateCollectionRepository<ReminderBD, ReminderBD>>(UpdateCollectionRepositoryType.REMINDERS.qualifier) {
            RemindersCollectionRepository(get(), get())
        }
        single<UpdateCollectionRepository<ExpenseBD, ExpenseBD>>(UpdateCollectionRepositoryType.EXPENSES.qualifier) {
            ExpensesCollectionRepository(get(), get())
        }
        single<UpdateCollectionRepository<IngredientBD, IngredientBD>>(
            UpdateCollectionRepositoryType.INGREDIENTS.qualifier
        ) {
            IngredientsCollectionRepository(get(), get())
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

private class TransactionCreateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncCreateSingleItemRepository<Transact>(
    tableName = TRANSACTION_TABLE_NAME,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {
    private val dao = db.transactionDao

    override suspend fun validate(item: Transact): Result<Unit> = dao.isCanSave(item)

    override suspend fun createInDb(item: Transact): Int = dao.create(item).toInt()
}

private class TransactionUpdateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateSingleLineRepository<Transact>(
    tableName = TRANSACTION_TABLE_NAME,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.transactionDao

    override suspend fun getInit(id: Int): Result<Transact> {
        return runCatching {
            dao.getTransaction(id)
        }
    }

    override fun prepareForUpdate(item: Transact): Transact = item.copy(updatedAt = defaultUpdatedAt())

    override suspend fun validate(item: Transact): Result<Unit> = dao.isCanSave(item)

    override suspend fun updateInDb(item: Transact) {
        dao.updateTransaction(item)
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
                ndsPercent = buy.ndsPercent,
                id = buy.id
            )
            buyDao.upsertBuyBd(buyBdIn)
        }
    }
}


private class RemindersCollectionRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateCollectionRepository<ReminderBD, ReminderBD>(
    tableName = REMINDER_TABLE_NAME,
    entitySyncKeyOf = ReminderBD::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    enqueueSyncDelete = syncQueueRepository::enqueueDelete,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.reminderDao

    override suspend fun getInit(id: Int): Result<List<ReminderBD>> {
        return runCatching {
            dao.getByTransactionId(id)
        }
    }

    override fun prepareForUpsert(item: ReminderBD): ReminderBD {
        return item.copy(updatedAt = defaultUpdatedAt())
    }

    override suspend fun deleteByIds(ids: List<Int>) {
        dao.deleteByIds(ids)
    }

    override suspend fun upsertItems(items: List<ReminderBD>) {
        dao.upsertAll(items)
    }
}


private class ExpensesCollectionRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateCollectionRepository<ExpenseBD, ExpenseBD>(
    tableName = EXPENSE_TABLE_NAME,
    entitySyncKeyOf = ExpenseBD::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    enqueueSyncDelete = syncQueueRepository::enqueueDelete,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.expenseDao

    override suspend fun getInit(id: Int): Result<List<ExpenseBD>> {
        return runCatching {
            dao.getByTransactionId(id)
        }
    }

    override fun prepareForUpsert(item: ExpenseBD): ExpenseBD {
        return item.copy(updatedAt = defaultUpdatedAt())
    }

    override suspend fun deleteByIds(ids: List<Int>) {
        dao.deleteByIds(ids)
    }

    override suspend fun upsertItems(items: List<ExpenseBD>) {
        dao.upsertAll(items)
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
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateCollectionRepository<IngredientBD, IngredientBD>(
    tableName = BATCH_MOVEMENT_TABLE_NAME,
    entitySyncKeyOf = IngredientBD::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    enqueueSyncDelete = syncQueueRepository::enqueueDelete,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val ingredientDao = db.ingredientDao
    private val movementDao = db.batchMovementDao

    override suspend fun getInit(id: Int): Result<List<IngredientBD>> {
        return runCatching {
            ingredientDao.getByTransactionId(id)
        }
    }

    override suspend fun deleteByIds(ids: List<Int>) {
        movementDao.deleteByIds(ids)
    }

    override suspend fun upsertItems(items: List<IngredientBD>) {
        val movements = items.map { ingredient ->
            BatchMovement(
                batchId = ingredient.batchId,
                movementType = MovementType.OUTGOING,
                count = ingredient.count,
                transactionId = ingredient.transactionId,
                id = ingredient.movementId,
                syncId = ingredient.syncId,
                updatedAt = defaultUpdatedAt(),
            )
        }
        movementDao.upsertMovements(movements)
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
                ndsPercent = sale.ndsPercent,
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
        val updatedBatchIds = when (transaction.transactionType) {
            TransactionType.BUY -> upsertBatchCostFromBuy(transactionId)
            TransactionType.OPZS -> upsertBatchCostFromOpzs(transactionId)
            else -> emptyList()
        }
        if (updatedBatchIds.isNotEmpty()) {
            recalculateDependentOpzs(updatedBatchIds)
        }
    }

    private suspend fun upsertBatchCostFromBuy(transactionId: Int): List<Int> {
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
        return batchCostPriceEntities.map { it.batchId }
    }

    /**
     * Пересчитывает себестоимость PF-партии (результат OPZS) по фактически списанным ингредиентам.
     *
     * Себестоимость PF = суммарная стоимость всех ингредиентов / выход PF (в кг).
     * Стоимость каждого ингредиента берётся из batch_cost_price (должна быть уже рассчитана).
     *
     * @return список из одного batch ID — PF-партии, для которой обновлена себестоимость.
     *         Пустой список если у OPZS нет INCOMING движения (нет выхода).
     */
    private suspend fun upsertBatchCostFromOpzs(transactionId: Int): List<Int> {
        // Разделяем движения: INCOMING = PF (выход), OUTGOING = ингредиенты (списание)
        val (pf, ingredients) = batchMovementDao.getByTransactionId(transactionId)
            .partition { it.movement.movementType == MovementType.INCOMING }
        if (pf.isEmpty()) return emptyList()
        val pfMovement = pf.first()

        // Загружаем себестоимость каждого ингредиента из batch_cost_price
        val ingredientsCostMap = ingredients
            .map { it.movement.batchId }
            .let { batchCostDao.getBatchesCostPriceByIds(it) }
            .associateBy { it.batchId }

        // Считаем суммарную стоимость всех списанных ингредиентов (коп/кг * грамм / 1000 = копейки)
        val totalCost = ingredients.sumOf { ingredient ->
            val costPerKg = ingredientsCostMap[ingredient.movement.batchId]?.costPricePerUnit ?: 0L
            (costPerKg * ingredient.movement.count) / 1000.0
        }

        // Себестоимость 1 кг PF = суммарная стоимость / выход в граммах * 1000
        val costPricePerKg = if (pfMovement.movement.count > 0) {
            (totalCost / pfMovement.movement.count.toDouble()) * 1000
        } else 0.0
        val pfBatchId = pfMovement.movement.batchId
        batchCostDao.upsert(
            listOf(
                BatchCostPriceEntity(pfBatchId, costPricePerKg.roundToLong())
            )
        )
        return listOf(pfBatchId)
    }
    /**
     * Каскадно пересчитывает себестоимость всех OPZS, зависящих от обновлённых батчей.
     *
     * Пример цепочки: Соль(BUY) → Баварские(OPZS) → Эластен(OPZS)
     * 1. updatedBatchIds = [batch соли]
     * 2. Ищем OPZS потребляющие соль → находим Баварские → пересчитываем → получаем batch Баварских
     * 3. Ищем OPZS потребляющие Баварские → находим Эластен → пересчитываем → получаем batch Эластена
     * 4. Ищем OPZS потребляющие Эластен → никого нет → цикл заканчивается
     */
    private suspend fun recalculateDependentOpzs(updatedBatchIds: List<Int>) {
        // Уже обработанные OPZS, чтобы не пересчитывать дважды
        val processedTransactions = mutableSetOf<Int>()
        // Батчи, для которых нужно найти потребителей в текущей итерации
        var batchIdsToProcess = updatedBatchIds.toSet()

        while (batchIdsToProcess.isNotEmpty()) {
            // Ищем OPZS-транзакции, которые потребляют наши батчи как ингредиенты
            val opzsTransactionIds = batchMovementDao
                .getOpzsTransactionIdsByIngredientBatchIds(batchIdsToProcess.toList())
            val newUpdatedBatchIds = mutableListOf<Int>()

            for (opzsId in opzsTransactionIds) {
                // Пропускаем если уже пересчитывали эту OPZS
                if (opzsId !in processedTransactions) {
                    processedTransactions.add(opzsId)
                    // Пересчитываем себестоимость этой OPZS
                    // upsertBatchCostFromOpzs вернёт batch ID её PF-партии
                    newUpdatedBatchIds.addAll(upsertBatchCostFromOpzs(opzsId))
                }
            }

            // PF-батчи пересчитанных OPZS — это новые батчи для следующей итерации
            // Возможно, другие OPZS потребляют их как ингредиенты
            batchIdsToProcess = newUpdatedBatchIds.toSet()
        }
    }
}

