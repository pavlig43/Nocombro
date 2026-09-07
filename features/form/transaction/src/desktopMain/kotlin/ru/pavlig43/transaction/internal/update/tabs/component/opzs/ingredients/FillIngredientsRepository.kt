package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.ingredient.IngredientBD
import kotlin.math.min

/** Подбирает партии сырья для заполнения ОПЗС по составу ПФ. */
internal class FillIngredientsRepository(
    db: NocombroDatabase,
) {
    private val compositionDao = db.compositionDao
    private val batchMovementDao = db.batchMovementDao

    /**
     * Загружает состав ПФ и остатки сырья, затем считает все строки в памяти.
     *
     * @param productId продукт ПФ
     * @param transactionId ID правимой ОПЗС; её движения не входят в остаток
     * @param countPf вес ПФ в граммах
     * @return готовые строки либо полный список дефицитов
     */
    suspend fun fillIngredientsFromComposition(
        productId: Int,
        transactionId: Int,
        countPf: Long,
    ): Result<FillIngredientsResult> = runCatching {
        val requiredIngredients = compositionDao
            .getIngredientsFromComposition(productId, transactionId, countPf)
            .mergeSameProducts()
        val movements = if (requiredIngredients.isEmpty()) {
            emptyList()
        } else {
            batchMovementDao.getActiveMainMovementsForProducts(
                productIds = requiredIngredients.map(IngredientBD::productId),
                excludedTransactionId = transactionId,
            )
        }
        val availableBatches = movements
            .groupBy { it.movement.batchId }
            .values
            .mapNotNull { batchMovements ->
                val balance = batchMovements.sumOf { movementOut ->
                    when (movementOut.movement.movementType) {
                        MovementType.INCOMING -> movementOut.movement.count
                        MovementType.OUTGOING -> -movementOut.movement.count
                    }
                }
                if (balance <= 0L) return@mapNotNull null

                val batchOut = batchMovements.first().batchOut
                AvailableIngredientBatch(
                    batchId = batchOut.batch.id,
                    productId = batchOut.product.id,
                    productName = batchOut.product.displayName,
                    productType = batchOut.product.type,
                    vendorName = batchOut.declaration.vendorName,
                    dateBorn = batchOut.batch.dateBorn,
                    balance = balance,
                )
            }

        allocateIngredients(
            transactionId = transactionId,
            requiredIngredients = requiredIngredients,
            availableBatches = availableBatches,
        )
    }
}

/** Итог полного расчёта до смены строк формы. */
internal sealed interface FillIngredientsResult {
    /** Все строки сырья готовы к подстановке в таблицу. */
    data class Ready(val ingredients: List<IngredientBD>) : FillIngredientsResult

    /** Одного или нескольких продуктов не хватает. */
    data class Deficit(val deficits: List<IngredientDeficit>) : FillIngredientsResult
}

/**
 * Нехватка сырья для одного продукта.
 *
 * @property missingCount недостающий вес в граммах
 */
internal data class IngredientDeficit(
    val productId: Int,
    val productName: String,
    val missingCount: Long,
)

/** Партия сырья с доступным остатком на основном складе. */
internal data class AvailableIngredientBatch(
    val batchId: Int,
    val productId: Int,
    val productName: String,
    val productType: ProductType,
    val vendorName: String,
    val dateBorn: LocalDate,
    val balance: Long,
)

/**
 * Раскладывает нужный вес по партиям: сперва по дате выпуска, затем по ID партии.
 * Возвращает готовые строки лишь тогда, когда сырья хватает для всего состава.
 */
internal fun allocateIngredients(
    transactionId: Int,
    requiredIngredients: List<IngredientBD>,
    availableBatches: List<AvailableIngredientBatch>,
): FillIngredientsResult {
    val batchesByProduct = availableBatches
        .groupBy(AvailableIngredientBatch::productId)
        .mapValues { (_, batches) ->
            batches.sortedWith(
                compareBy(AvailableIngredientBatch::dateBorn, AvailableIngredientBatch::batchId)
            )
        }

    val ingredients = mutableListOf<IngredientBD>()
    val deficits = mutableListOf<IngredientDeficit>()

    requiredIngredients.forEach { required ->
        var remaining = required.count
        for (batch in batchesByProduct[required.productId].orEmpty()) {
            if (remaining <= 0L) break
            val count = min(batch.balance, remaining)
            ingredients += IngredientBD(
                transactionId = transactionId,
                batchId = batch.batchId,
                dateBorn = batch.dateBorn,
                movementId = 0,
                count = count,
                productType = batch.productType,
                productId = batch.productId,
                productName = batch.productName,
                vendorName = batch.vendorName,
                id = 0,
            )
            remaining -= count
        }

        if (remaining > 0L) {
            deficits += IngredientDeficit(
                productId = required.productId,
                productName = required.productName,
                missingCount = remaining,
            )
        }
    }

    return if (deficits.isEmpty()) {
        FillIngredientsResult.Ready(ingredients)
    } else {
        FillIngredientsResult.Deficit(deficits)
    }
}

/** Складывает вес строк состава, которые ссылаются на один продукт. */
private fun List<IngredientBD>.mergeSameProducts(): List<IngredientBD> =
    groupBy(IngredientBD::productId).values.map { ingredients ->
        ingredients.first().copy(count = ingredients.sumOf(IngredientBD::count))
    }
