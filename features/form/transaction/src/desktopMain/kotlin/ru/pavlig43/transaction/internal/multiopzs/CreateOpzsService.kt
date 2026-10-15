package ru.pavlig43.transaction.internal.multiopzs

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.ingredient.IngredientBD
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.database.inTransaction
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.transaction.internal.di.BatchCostRepository
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients.FillIngredientsRepository
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients.FillIngredientsResult

/** Создаёт одну проведённую ОПЗС; все вложенные записи откатываются вместе. */
internal class CreateOpzsService(
    private val db: NocombroDatabase,
    private val fillIngredients: FillIngredientsRepository,
    private val pfRepository: UpdateSingleLineRepository<PfBD>,
    private val ingredientsRepository: UpdateCollectionRepository<IngredientBD, IngredientBD>,
    private val batchCostRepository: BatchCostRepository,
) {
    suspend fun declarations(productId: Int): List<Declaration> =
        db.productDeclarationDao.getProductDeclarationIn(productId)
            .map { it.declarationId }.distinct()
            .map { db.declarationDao.getDeclaration(it) }

    suspend fun validate(row: MultiOpzsRow) {
        require(row.productId != 0) { "Выберите продукт" }
        db.productDao.getProduct(row.productId)
        require(row.count.value > 0) { "Вес должен быть больше нуля" }
        require(row.declarationId != 0) { "Выберите декларацию" }
        require(declarations(row.productId).any { it.id == row.declarationId }) {
            "Декларация не связана с продуктом"
        }
    }

    suspend fun create(row: MultiOpzsRow, producedAt: LocalDateTime): Int = db.inTransaction {
        // Проверяем до INSERT (DAO использует REPLACE): повтор не должен удалить старый граф.
        db.transactionDao.getTransactionBySyncId(row.syncId)?.let { existing ->
            check(existing.deletedAt == null && existing.isCompleted &&
                existing.transactionType == TransactionType.OPZS) { "ОПЗС с этим sync_id недоступна" }
            return@inTransaction existing.id
        }
        validate(row)
        val id = db.transactionDao.create(
            Transact(
                transactionType = TransactionType.OPZS,
                createdAt = producedAt,
                comment = "",
                isCompleted = true,
                syncId = row.syncId,
            )
        ).toInt()
        val ingredients = when (val result = fillIngredients.fillIngredientsFromComposition(
            productId = row.productId, transactionId = id, countPf = row.count.value,
        ).getOrThrow()) {
            is FillIngredientsResult.Ready -> result.ingredients
            is FillIngredientsResult.Deficit -> error(result.deficits.joinToString("; ") {
                "${it.productName}: не хватает ${ru.pavlig43.core.model.DecimalData3(it.missingCount)} кг"
            })
        }
        require(ingredients.isNotEmpty() && ingredients.any { it.count > 0 }) {
            "Пустой состав: выпуск без сырья запрещён"
        }
        pfRepository.update(ChangeSet(null, PfBD(
            transactionId = id,
            productId = row.productId,
            declarationId = row.declarationId,
            dateBorn = producedAt.date,
            count = row.count.value,
        ))).getOrThrow()
        ingredientsRepository.update(ChangeSet(null, ingredients)).getOrThrow()
        batchCostRepository.updateBatchCost(id)
        id
    }
}
