package ru.pavlig43.transaction.internal.production

import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.production.ProductionPlan
import ru.pavlig43.database.data.production.ProductionPlanRow
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

internal data class PlanDraft(val title: String = "", val lines: List<PlanLine> = listOf(PlanLine()))

internal class ProductionPlanRepository(private val db: NocombroDatabase) {
    private val dao = db.productionPlanDao
    private val deletions = MirrorDeletionJournalRepository(db)
    val plans = dao.observePlans()

    suspend fun load(id: Int): PlanDraft = db.inTransaction {
        val plan = requirePlan(id)
        PlanDraft(plan.title, dao.getRows(id).map {
            PlanLine(
                it.syncId, it.productId, db.productDao.getProduct(it.productId).displayName,
                kgText(it.plannedGrams), kgText(it.packageGrams),
            )
        })
    }

    suspend fun save(id: Int?, draft: PlanDraft): Int = db.inTransaction {
        require(draft.title.isNotBlank()) { "Введите название плана" }
        require(draft.lines.isNotEmpty()) { "Добавьте хотя бы одну строку" }
        val existing = id?.let { requirePlan(it) }
        val oldRows = existing?.let { dao.getRows(it.id) }.orEmpty()
        val oldBySyncId = oldRows.associateBy { it.syncId }
        val rows = draft.lines.mapIndexed { position, line ->
            val old = oldBySyncId[line.syncId]
            try {
                require(line.productId != 0) { "Выберите продукт" }
                val product = db.productDao.getProduct(line.productId)
                val planned = requireNotNull(parseGrams(line.planned)) { "Введите плановый вес с точностью до грамма" }
                val pack = requireNotNull(parseGrams(line.packageWeight)) { "Введите вес пакета с точностью до грамма" }
                splitBatches(planned, product.requireMixCapacity(), pack)
                val candidate = ProductionPlanRow(
                    planId = id ?: 0, productId = line.productId, position = position,
                    plannedGrams = planned, packageGrams = pack,
                    id = old?.id ?: 0, syncId = line.syncId, updatedAt = old?.updatedAt ?: defaultUpdatedAt(),
                )
                if (old != null && candidate != old) candidate.copy(updatedAt = defaultUpdatedAt(old.updatedAt)) else candidate
            } catch (error: IllegalArgumentException) {
                throw IllegalArgumentException("Строка ${position + 1}: ${error.message}", error)
            }
        }
        val title = draft.title.trim()
        val planId = if (existing == null) {
            dao.insertPlan(ProductionPlan(title, getCurrentLocalDateTime())).toInt()
        } else {
            if (title != existing.title) dao.upsertPlan(existing.copy(title = title, updatedAt = defaultUpdatedAt(existing.updatedAt)))
            existing.id
        }
        val retained = rows.mapTo(mutableSetOf()) { it.syncId }
        val removed = oldRows.filterNot { it.syncId in retained }.map { it.id }
        if (removed.isNotEmpty()) {
            deletions.captureHardDeletesInCurrentTransaction { dao.deleteRows(removed) }
        }
        dao.upsertRows(rows.map { it.copy(planId = planId) }.filter { it != oldBySyncId[it.syncId] })
        planId
    }

    suspend fun copy(id: Int): Int = db.inTransaction {
        val plan = requirePlan(id)
        val rows = dao.getRows(id)
        val newId = dao.insertPlan(ProductionPlan("${plan.title} (копия)", getCurrentLocalDateTime())).toInt()
        dao.upsertRows(rows.map {
            it.copy(id = 0, planId = newId, syncId = defaultSyncId(), updatedAt = defaultUpdatedAt())
        })
        newId
    }

    suspend fun delete(id: Int) = deletions.captureHardDeletes { dao.deletePlan(id) }

    private suspend fun requirePlan(id: Int): ProductionPlan =
        requireNotNull(dao.getPlan(id)?.takeIf { it.deletedAt == null }) { "План производства недоступен" }
}

internal fun Product.requireMixCapacity(): Long {
    require(deletedAt == null && type == ProductType.FOOD_PF) { "Выберите действующий пищевой П/Ф" }
    return requireNotNull(maxMixGrams?.takeIf { it > 0 }) { "Укажите максимальный замес в карточке продукта" }
}

internal fun parseGrams(text: String): Long? = text.trim().replace(',', '.')
    .takeIf { it.matches(Regex("[0-9]+(\\.[0-9]{0,3})?")) }
    ?.toBigDecimalOrNull()
    ?.movePointRight(3)
    ?.let { runCatching { it.longValueExact() }.getOrNull() }

internal fun kgText(grams: Long): String =
    java.math.BigDecimal.valueOf(grams, 3).stripTrailingZeros().toPlainString().replace('.', ',')
