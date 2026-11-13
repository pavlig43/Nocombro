package ru.pavlig43.transaction.internal.production

import ru.pavlig43.database.NocombroDatabase
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
    require(capacity >= packageGrams) { "В мешалку не входит один пакет" }
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

internal data class ProductionLayout(val title: String, val products: List<ProductLayout>)

internal suspend fun NocombroDatabase.readProductionLayout(planId: Int): ProductionLayout = inTransaction {
    val plan = requireNotNull(productionPlanDao.getPlan(planId)?.takeIf { it.deletedAt == null }) {
        "План производства недоступен"
    }
    val rows = productionPlanDao.getRows(planId)
    require(rows.isNotEmpty()) { "В плане нет строк" }
    ProductionLayout(plan.title, rows.map { row ->
        val product = productDao.getProduct(row.productId)
        val name = product.displayName
        try {
            val capacity = product.requireMixCapacity()
            val batches = splitBatches(row.plannedGrams, capacity, row.packageGrams)
            val recipe = compositionDao.getActiveCompositionOut(product.id)
                .filter { it.productType in setOf(ProductType.FOOD_BASE, ProductType.FOOD_PF) }
            require(recipe.isNotEmpty() && recipe.all { it.count in 1..1000 } && recipe.sumOf { it.count } == 1000L) {
                "Пищевой состав должен давать 1 кг на 1 кг выпуска"
            }
            ProductLayout(
                name, row.plannedGrams, capacity, row.packageGrams, batches,
                recipe.groupBy { it.productId }.values.map { items ->
                    RecipeIngredient(items.first().productName, items.sumOf { it.count })
                },
            )
        } catch (error: IllegalArgumentException) {
            throw IllegalArgumentException("$name: ${error.message}", error)
        }
    })
}
