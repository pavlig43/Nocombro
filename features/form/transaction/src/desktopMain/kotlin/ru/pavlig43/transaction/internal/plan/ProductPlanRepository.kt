package ru.pavlig43.transaction.internal.plan

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.plan.ProductPlan
import ru.pavlig43.database.data.product.plan.ProductPlanRow
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.sync.mirror.MirrorDeletionJournalRepository
import ru.pavlig43.database.inTransaction
import ru.pavlig43.datetime.getCurrentLocalDateTime

internal data class PlanLine(
    val syncId: String = defaultSyncId(),
    val productId: Int = 0,
    val productName: String = "",
    val planned: String = "",
    val packageWeight: String = "3",
)

internal data class PlanDraft(
    val lines: List<PlanLine> = listOf(PlanLine()),
    val createdAt: LocalDateTime? = null,
)

internal class ProductPlanRepository(private val db: NocombroDatabase) {
    private val dao = db.productPlanDao
    private val deletions = MirrorDeletionJournalRepository(db)
    val products = db.productDao.observeOnProducts()

    suspend fun load(id: Int): PlanDraft = db.inTransaction {
        val plan = requirePlan(id)
        PlanDraft(
            lines = dao.getRows(id).map {
                PlanLine(
                    it.syncId, it.productId, db.productDao.getProduct(it.productId).displayName,
                    kgText(it.plannedGrams), kgText(it.packageGrams),
                )
            },
            createdAt = plan.createdAt,
        )
    }

    suspend fun save(id: Int?, draft: PlanDraft, rowNumberAt: (Int) -> Int = { it + 1 }): Int = db.inTransaction {
        require(draft.lines.isNotEmpty()) { "Добавьте хотя бы одну строку" }
        val existing = id?.let { requirePlan(it) }
        val oldRows = existing?.let { dao.getRows(it.id) }.orEmpty()
        val oldBySyncId = oldRows.associateBy { it.syncId }
        val rows = draft.lines.mapIndexed { position, line ->
            val old = oldBySyncId[line.syncId]
            try {
                val product = line.productId.takeIf { it != 0 }?.let { db.productDao.getProduct(it) }
                val (planned, pack) = line.checkedWeights(product)
                val candidate = ProductPlanRow(
                    planId = id ?: 0, productId = line.productId, position = position,
                    plannedGrams = planned, packageGrams = pack,
                    id = old?.id ?: 0, syncId = line.syncId, updatedAt = old?.updatedAt ?: defaultUpdatedAt(),
                )
                if (old != null && candidate != old) candidate.copy(updatedAt = defaultUpdatedAt(old.updatedAt)) else candidate
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException(planLineErrorMessage(rowNumberAt(position), error.message.orEmpty()), error)
            }
        }
        val planId = existing?.id ?: dao.insertPlan(ProductPlan(getCurrentLocalDateTime())).toInt()
        val retained = rows.mapTo(mutableSetOf()) { it.syncId }
        val removed = oldRows.filterNot { it.syncId in retained }.map { it.id }
        if (removed.isNotEmpty()) {
            deletions.captureHardDeletesInCurrentTransaction { dao.deleteRows(removed) }
        }
        dao.upsertRows(rows.map { it.copy(planId = planId) }.filter { it != oldBySyncId[it.syncId] })
        planId
    }

    private suspend fun requirePlan(id: Int): ProductPlan =
        requireNotNull(dao.getPlan(id)?.takeIf { it.deletedAt == null }) { "План производства недоступен" }
}

internal fun planLineErrorMessage(rowNumber: Int, message: String): String = "В строке $rowNumber: $message"

internal fun PlanLine.validationError(product: Product?): String? =
    try {
        checkedWeights(product)
        null
    } catch (error: IllegalArgumentException) {
        error.message
    }

private fun PlanLine.checkedWeights(product: Product?): Pair<Long, Long> {
    require(productId != 0) { "Выберите продукт" }
    requireNotNull(product) { "Продукт недоступен" }
    val plannedGrams = requireNotNull(parseGrams(planned)) { "Введите плановый вес с точностью до грамма" }
    val pack = requireNotNull(parseGrams(packageWeight)) { "Введите вес пакета с точностью до грамма" }
    splitBatches(plannedGrams, product.requireMixCapacity(), pack)
    return plannedGrams to pack
}

internal fun Product.requireMixCapacity(): Long {
    require(deletedAt == null && type == ProductType.FOOD_PF) { "Выберите действующий пищевой П/Ф" }
    return requireNotNull(maxMixGrams?.takeIf { it > 0 }) { "Укажите массу замеса в карточке продукта" }
}

internal fun parseGrams(text: String): Long? = text.trim().replace(',', '.')
    .takeIf { it.matches(Regex("[0-9]+(\\.[0-9]{0,3})?")) }
    ?.toBigDecimalOrNull()
    ?.movePointRight(3)
    ?.let { runCatching { it.longValueExact() }.getOrNull() }

internal fun kgText(grams: Long): String =
    java.math.BigDecimal.valueOf(grams, 3).stripTrailingZeros().toPlainString().replace('.', ',')
