package ru.pavlig43.transaction.internal.plan

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.inTransaction

internal data class BatchSplit(
    val batchCount: Long,
    val packagesPerBatch: Long,
    val largerBatchCount: Long,
) {
    fun packagesAt(index: Long): Long = packagesPerBatch + if (index < largerBatchCount) 1 else 0
}

internal fun splitBatches(planned: Long, capacity: Long, packageGrams: Long): BatchSplit {
    require(planned > 0) { "Плановый вес должен быть больше нуля" }
    require(packageGrams > 0) { "Вес пакета должен быть больше нуля" }
    require(capacity >= packageGrams) { "Вес пакета больше массы замеса" }
    val packages = planned / packageGrams
    val remainder = planned % packageGrams
    require(remainder == 0L) {
        val lower = (planned - remainder).takeIf { it > 0 }
        val delta = packageGrams - remainder
        val upper = if (planned <= Long.MAX_VALUE - delta) planned + delta else null
        val alternatives = listOfNotNull(lower, upper).joinToString(" и ") { "${kgText(it)} кг" }
        "План должен быть кратен весу пакета. Ближайший допустимый вес: $alternatives"
    }
    val maxPackages = capacity / packageGrams
    val batches = 1 + (packages - 1) / maxPackages
    return BatchSplit(batches, packages / batches, packages % batches)
}

internal data class RecipeIngredient(val name: String, val gramsPerKg: Long)

internal fun weighIngredients(recipe: List<RecipeIngredient>, batchGrams: Long): List<Long> {
    val fractions = recipe.map { batchGrams % 1000 * it.gramsPerKg }
    val weights = recipe.mapIndexed { i, ingredient ->
        batchGrams / 1000 * ingredient.gramsPerKg + fractions[i] / 1000
    }
    val residual = (batchGrams - weights.sum()).toInt()
    val roundUp = recipe.indices.sortedByDescending { fractions[it] % 1000 }.take(residual).toSet()
    return weights.mapIndexed { i, weight -> if (i in roundUp) weight + 1 else weight }
}

internal data class ProductLayout(
    val name: String,
    val plannedGrams: Long,
    val capacity: Long,
    val packageGrams: Long,
    val batches: BatchSplit,
    val recipe: List<RecipeIngredient>,
)

internal data class ProductPlanLayout(val createdAt: LocalDateTime, val products: List<ProductLayout>)

internal val ProductType.isFoodIngredient: Boolean
    get() = this == ProductType.FOOD_BASE || this == ProductType.FOOD_PF

private fun List<CompositionOut>.toPlanRecipe(): Map<Int, RecipeIngredient>? {
    val food = filter { it.productType.isFoodIngredient }
    if (food.any { it.count !in 1..1000 } || food.sumOf { it.count } != 1000L) return null
    return food.groupBy { it.productId }.mapValues { (_, items) ->
        RecipeIngredient(items.first().productName, items.sumOf { it.count })
    }
}

/** Суммирует навески всех замесов для веса выпуска и фасовки из плана. */
internal suspend fun NocombroDatabase.plannedFoodWeights(
    productId: Int,
    producedGrams: Long,
    packageGrams: Long,
): Map<Int, Long>? {
    val product = productDao.getProduct(productId)
    val batches = try {
        splitBatches(producedGrams, product.requireMixCapacity(), packageGrams)
    } catch (_: IllegalArgumentException) {
        return null
    }
    val recipeByProduct = compositionDao.getActiveCompositionOut(productId).toPlanRecipe() ?: return null
    val recipe = recipeByProduct.values.toList()
    val regularWeights = weighIngredients(recipe, batches.packagesPerBatch * packageGrams)
    val largerWeights = if (batches.largerBatchCount > 0) {
        weighIngredients(recipe, batches.packagesAt(0) * packageGrams)
    } else {
        regularWeights
    }
    val regularCount = batches.batchCount - batches.largerBatchCount
    return recipeByProduct.keys.withIndex().associate { (index, id) ->
        id to (regularWeights[index] * regularCount + largerWeights[index] * batches.largerBatchCount)
    }
}

internal suspend fun NocombroDatabase.readProductPlanLayout(planId: Int): ProductPlanLayout = inTransaction {
    val plan = requireNotNull(productPlanDao.getPlan(planId)?.takeIf { it.deletedAt == null }) {
        "План производства недоступен"
    }
    val rows = productPlanDao.getRows(planId)
    require(rows.isNotEmpty()) { "В плане нет строк" }
    ProductPlanLayout(plan.createdAt, rows.map { row ->
        val product = productDao.getProduct(row.productId)
        val name = product.displayName
        try {
            val capacity = product.requireMixCapacity()
            val batches = splitBatches(row.plannedGrams, capacity, row.packageGrams)
            val recipe = requireNotNull(compositionDao.getActiveCompositionOut(product.id).toPlanRecipe()) {
                "Пищевой состав должен давать 1 кг на 1 кг выпуска"
            }
            ProductLayout(name, row.plannedGrams, capacity, row.packageGrams, batches, recipe.values.toList())
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("$name: ${error.message}", error)
        }
    })
}
