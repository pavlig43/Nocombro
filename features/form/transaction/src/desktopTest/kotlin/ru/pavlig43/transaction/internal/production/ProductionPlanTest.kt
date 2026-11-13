package ru.pavlig43.transaction.internal.production

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import java.nio.file.Files

class ProductionPlanTest : FunSpec({
    test("minimum count of balanced whole-package batches") {
        fun batches(total: Long, limit: Long, pack: Long): List<Long> {
            val split = splitBatches(total, limit, pack)
            return (0 until split.batchCount).map { split.packagesAt(it) * pack }
        }
        batches(21_000, 18_000, 3_000) shouldBe listOf(12_000L, 9_000L)
        batches(48_000, 36_000, 3_000) shouldBe listOf(24_000L, 24_000L)
        batches(48_000, 18_000, 3_000) shouldBe listOf(18_000L, 15_000L, 15_000L)
        batches(36_000, 17_000, 3_000) shouldBe listOf(12_000L, 12_000L, 12_000L)
        batches(3_000, 18_000, 3_000) shouldBe listOf(3_000L)
        val nonMultiple = runCatching { batches(20_000, 18_000, 3_000) }.exceptionOrNull()?.message.orEmpty()
        ("18" in nonMultiple && "21" in nonMultiple) shouldBe true
        runCatching { batches(12_000, 2_000, 3_000) }.isFailure shouldBe true
        parseGrams("3,001") shouldBe 3001L
        parseGrams("3,0001") shouldBe null
    }

    test("rounding gives the gram to the largest remainder and preserves batch weight") {
        val recipe = listOf(RecipeIngredient("А", 333), RecipeIngredient("Б", 333), RecipeIngredient("В", 334))
        weighIngredients(recipe, 1001) shouldBe listOf(333L, 333L, 335L)
        weighIngredients(recipe, 1501).sum() shouldBe 1501L
        weighIngredients(recipe, Long.MAX_VALUE).sum() shouldBe Long.MAX_VALUE
    }

    test("saved plan prints the current direct food composition and survives mirror restoration") {
        withEmptyTestDatabase { db ->
            val date = LocalDate(2026, 1, 1)
            val pfId = db.productDao.create(Product(ProductType.FOOD_PF, "Эластен", createdAt = date, maxMixGrams = 18_000)).toInt()
            val baseId = db.productDao.create(Product(ProductType.FOOD_BASE, "Базовый", createdAt = date)).toInt()
            val otherPfId = db.productDao.create(Product(ProductType.FOOD_PF, "Компонент П/Ф", createdAt = date)).toInt()
            val packId = db.productDao.create(Product(ProductType.PACK, "Упаковка", createdAt = date)).toInt()
            db.compositionDao.upsertComposition(listOf(
                CompositionIn(0, pfId, baseId, 700),
                CompositionIn(0, pfId, otherPfId, 300),
                CompositionIn(0, pfId, packId, 500),
            ))
            val repo = ProductionPlanRepository(db)
            val line = PlanLine(productId = pfId, planned = "21")
            val id = repo.save(null, PlanDraft("Выпуск", listOf(line)))
            val copyId = repo.copy(id)
            (repo.load(copyId).lines.single().syncId != line.syncId) shouldBe true
            val productLayout = db.readProductionLayout(id).products.single()
            productLayout.recipe.map { it.name } shouldBe listOf("Базовый", "Компонент П/Ф")
            productLayout.batches.packagesAt(0) shouldBe 4L
            val product = db.productDao.getProduct(pfId)
            db.productDao.updateProduct(product.copy(maxMixGrams = 9_000, updatedAt = defaultUpdatedAt(product.updatedAt)))
            db.readProductionLayout(id).products.single().batches.batchCount shouldBe 3L
            val recipe = db.compositionDao.getAll().filter { it.productId != packId }
            db.compositionDao.upsertComposition(recipe.map { it.copy(count = if (it.productId == baseId) 600 else 400) })
            db.readProductionLayout(id).products.single().recipe.map { it.gramsPerKg } shouldBe listOf(600L, 400L)

            val tables = listOf(MirrorSyncTable.PRODUCT, MirrorSyncTable.PRODUCTION_PLAN, MirrorSyncTable.PRODUCTION_PLAN_ROW)
            val snapshot = MirrorLocalSnapshotRepository(db).loadSnapshot(tables)
            withEmptyTestDatabase { fresh ->
                fresh.productDao.create(Product(ProductType.FOOD_BASE, "Другой локальный ID", createdAt = date))
                val apply = MirrorLocalApplyRepository(fresh, MirrorEntityApplyRepository(fresh))
                apply.apply(snapshot.rowsByTable.flatMap { (table, rows) -> rows.map { MirrorPushEntityChange(table, it) } })
                val syncId = requireNotNull(db.productionPlanDao.getPlan(id)).syncId
                val restored = requireNotNull(fresh.productionPlanDao.getPlanBySyncId(syncId))
                val restoredLine = fresh.productionPlanDao.getRows(restored.id).single()
                restoredLine.syncId shouldBe line.syncId
                fresh.productDao.getProduct(restoredLine.productId).maxMixGrams shouldBe 9000L
                repo.delete(id)
                val afterDelete = MirrorLocalSnapshotRepository(db).loadSnapshot(tables)
                apply.apply(afterDelete.rowsByTable.flatMap { (table, rows) -> rows.map { MirrorPushEntityChange(table, it) } })
                fresh.productionPlanDao.getPlanBySyncId(syncId) shouldBe null
                fresh.productionPlanDao.getRows(restored.id) shouldBe emptyList()
            }
        }
    }

    test("one PDF is replaced and a long Cyrillic recipe continues on later A4 pages") {
        val folder = Files.createTempDirectory("nocombro-production-test")
        try {
            val recipe = List(100) { RecipeIngredient("Ингредиент №${it + 1}", 10) }
            val product = ProductLayout("Реогард", 21_000, 18_000, 3_000, splitBatches(21_000, 18_000, 3_000), recipe)
            val path = replaceProductionPdf(ProductionLayout("Первая раскладка", listOf(product)), folder)
            replaceProductionPdf(ProductionLayout("Вторая раскладка", listOf(product)), folder) shouldBe path
            Loader.loadPDF(path.toFile()).use { pdf ->
                (pdf.numberOfPages > 1) shouldBe true
                pdf.getPage(0).mediaBox.width shouldBe org.apache.pdfbox.pdmodel.common.PDRectangle.A4.width
                val text = PDFTextStripper().getText(pdf)
                ("Вторая раскладка" in text) shouldBe true
                ("Первая раскладка" in text) shouldBe false
                ("Ингредиент №100" in text) shouldBe true
                ("Замес №2" in text) shouldBe true
            }
            Files.list(folder).use { it.count() shouldBe 1L }
        } finally {
            Files.list(folder).use { paths -> paths.forEach { Files.deleteIfExists(it) } }
            Files.deleteIfExists(folder)
        }
    }
})
