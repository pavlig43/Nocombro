package ru.pavlig43.product.internal.update.tabs.specification

import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.dao.CompositionDao
import ru.pavlig43.product.internal.update.tabs.composition.CompositionUi

/**
 * Собирает текстовое поле "Состав" для спецификации из вкладки состава продукта.
 *
 * Верхний уровень берется из текущего UI-состояния вкладки "Состав", чтобы в
 * генерацию попадали даже еще не сохраненные изменения пользователя. Все
 * вложенные полуфабрикаты разворачиваются через [CompositionDao] до базовых
 * продуктов, после чего одинаковые базовые продукты агрегируются и сортируются
 * по убыванию граммовки.
 */
internal class ProductSpecificationCompositionGenerator(
    private val compositionDao: CompositionDao,
) {

    /**
     * Возвращает строку состава вида `Ингредиент 1, Ингредиент 2, ...`.
     */
    suspend fun generate(
        productId: Int,
        currentComposition: List<CompositionUi>,
    ): Result<String> {
        return runCatching {
            val topLevelItems = currentComposition
                .takeIf { it.isNotEmpty() }
                ?.mapNotNull { it.toResolvedItemOrNull() }
                ?: compositionDao.getCompositionOut(productId)
                    .map(CompositionOut::toResolvedItem)

            val totals = linkedMapOf<Int, BaseCompositionTotal>()
            flattenToBaseProducts(
                items = topLevelItems,
                totals = totals,
                productPathIds = linkedSetOf(productId),
                productPathNames = mutableListOf(),
            )

            totals.values
                .sortedByDescending(BaseCompositionTotal::grams)
                .joinToString(", ") { it.name }
        }
    }

    private suspend fun flattenToBaseProducts(
        items: List<ResolvedCompositionItem>,
        totals: MutableMap<Int, BaseCompositionTotal>,
        productPathIds: LinkedHashSet<Int>,
        productPathNames: MutableList<String>,
    ) {
        items.forEach { item ->
            when (item.productType) {
                ProductType.FOOD_BASE -> {
                    val current = totals[item.productId]
                    if (current == null) {
                        totals[item.productId] = BaseCompositionTotal(
                            productId = item.productId,
                            name = item.productName,
                            grams = item.countGrams,
                        )
                    } else {
                        totals[item.productId] = current.copy(grams = current.grams + item.countGrams)
                    }
                }

                ProductType.FOOD_PF -> {
                    if (!productPathIds.add(item.productId)) {
                        val cyclePath = (productPathNames + item.productName).joinToString(" -> ")
                        error("Обнаружено зацикливание состава: $cyclePath")
                    }
                    productPathNames += item.productName

                    val nestedItems = compositionDao.getCompositionOut(item.productId)
                        .takeIf { it.isNotEmpty() }
                        ?: error("У полуфабриката ${item.productName} не заполнен состав.")

                    flattenToBaseProducts(
                        items = nestedItems.map { nested ->
                            nested.toResolvedItem(parentCountGrams = item.countGrams)
                        },
                        totals = totals,
                        productPathIds = productPathIds,
                        productPathNames = productPathNames,
                    )

                    productPathNames.removeLast()
                    productPathIds.remove(item.productId)
                }

                else -> Unit
            }
        }
    }
}

private data class ResolvedCompositionItem(
    val productId: Int,
    val productName: String,
    val productType: ProductType,
    val countGrams: Long,
)

private data class BaseCompositionTotal(
    val productId: Int,
    val name: String,
    val grams: Long,
)

private fun CompositionUi.toResolvedItemOrNull(): ResolvedCompositionItem? {
    val type = productType ?: return null
    return ResolvedCompositionItem(
        productId = productId,
        productName = productName,
        productType = type,
        countGrams = count.value,
    )
}

private fun CompositionOut.toResolvedItem(
    parentCountGrams: Long = 1000L,
): ResolvedCompositionItem {
    return ResolvedCompositionItem(
        productId = productId,
        productName = productName,
        productType = productType,
        countGrams = (count * parentCountGrams) / 1000,
    )
}
