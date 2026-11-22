package ru.pavlig43.transaction.internal.plan

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.text.PDFTextStripper
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.sync.mirror.MirrorDeletionJournalRepository
import ru.pavlig43.database.data.sync.mirror.MirrorEntityApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalApplyRepository
import ru.pavlig43.database.data.sync.mirror.MirrorLocalSnapshotRepository
import ru.pavlig43.database.data.sync.mirror.MirrorPushEntityChange
import ru.pavlig43.database.data.sync.mirror.MirrorSyncTable
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.loadinitdata.api.component.LoadInitDataState
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.api.component.ProductPlanComponent
import java.nio.file.Files
import java.nio.file.Path

class ProductPlanTest : FunSpec({
    test("standard plan table edits selected rows and reports their displayed numbers") {
        withEmptyTestDatabase { db ->
            val dependencies = TransactionFormDependencies(
                db, NocombroTransactionExecutor(db), ImmutableTableDependencies(db),
                FilesDependencies(db = db, remoteFileStorageGateway = NoopRemoteFileStorageGateway()),
            )
            val lifecycle = LifecycleRegistry()
            val openedProducts = mutableListOf<Int>()
            val opener = object : TabOpener by NoopTabOpener {
                override fun openProductTab(id: Int) { openedProducts.add(id) }
            }
            val component = runOnUiThread {
                ProductPlanComponent(DefaultComponentContext(lifecycle), 0, dependencies, opener, {}, {}, {})
            }
            try {
                val table = component.rows
                waitUntil { table.initDataComponent.loadState.value is LoadInitDataState.Success }
                runOnUiThread {
                    repeat(2) { table.onEvent(MutableUiEvent.CreateNewItem) }
                    val retained = table.itemList.value[1]
                    table.onEvent(MutableUiEvent.Selection(SelectionUiEvent.ToggleSelection(0)))
                    table.onEvent(MutableUiEvent.Selection(SelectionUiEvent.ToggleSelection(2)))
                    table.onEvent(MutableUiEvent.DeleteSelected)
                    table.onEvent(MutableUiEvent.UpdateItem(retained.copy(line = retained.line.copy(planned = "9"))))
                    table.itemList.value.single().line.planned shouldBe "9"
                    table.itemList.value.single().composeId shouldBe 1
                    component.openSelectedProduct(0)
                    component.openSelectedProduct(17)
                    openedProducts shouldBe listOf(17)
                }
                withTimeout(5_000) { table.errorMessages.first { it.isNotEmpty() } } shouldBe
                    listOf("В строке 2: Выберите продукт")
                runOnUiThread { component.save() }
                waitUntil { !component.state.value.busy && component.state.value.error != null }
                component.state.value.error shouldBe "В строке 2: Выберите продукт"

                val productId = db.productDao.create(Product(
                    ProductType.FOOD_PF, "Эластен", createdAt = LocalDate(2026, 1, 1), maxMixGrams = 18_000,
                )).toInt()
                runOnUiThread {
                    table.selectProduct(table.itemList.value.single().line.syncId, productId, "Эластен")
                    component.save()
                }
                waitUntil { component.state.value.id != null }
                val savedRows = db.productPlanDao.getRows(requireNotNull(component.state.value.id))
                savedRows.single().plannedGrams shouldBe 9_000L
                savedRows.single().productId shouldBe productId
            } finally {
                runOnUiThread { lifecycle.destroy() }
            }
        }
    }

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
            val repo = ProductPlanRepository(db)
            val line = PlanLine(productId = pfId, planned = "21")
            val id = repo.save(null, PlanDraft(lines = listOf(line)))
            val createdAt = requireNotNull(repo.load(id).createdAt)
            val productLayout = db.readProductPlanLayout(id).products.single()
            productLayout.recipe.map { it.name } shouldBe listOf("Базовый", "Компонент П/Ф")
            productLayout.batches.packagesAt(0) shouldBe 4L
            val product = db.productDao.getProduct(pfId)
            db.productDao.updateProduct(product.copy(maxMixGrams = 9_000, updatedAt = defaultUpdatedAt(product.updatedAt)))
            db.readProductPlanLayout(id).products.single().batches.batchCount shouldBe 3L
            val recipe = db.compositionDao.getAll().filter { it.productId != packId }
            db.compositionDao.upsertComposition(recipe.map { it.copy(count = if (it.productId == baseId) 600 else 400) })
            db.readProductPlanLayout(id).products.single().recipe.map { it.gramsPerKg } shouldBe listOf(600L, 400L)

            val tables = listOf(MirrorSyncTable.PRODUCT, MirrorSyncTable.PRODUCTION_PLAN, MirrorSyncTable.PRODUCTION_PLAN_ROW)
            val snapshot = MirrorLocalSnapshotRepository(db).loadSnapshot(tables)
            withEmptyTestDatabase { fresh ->
                fresh.productDao.create(Product(ProductType.FOOD_BASE, "Другой локальный ID", createdAt = date))
                val apply = MirrorLocalApplyRepository(fresh, MirrorEntityApplyRepository(fresh))
                apply.apply(snapshot.rowsByTable.flatMap { (table, rows) -> rows.map { MirrorPushEntityChange(table, it) } })
                val syncId = requireNotNull(db.productPlanDao.getPlan(id)).syncId
                val restored = requireNotNull(fresh.productPlanDao.getPlanBySyncId(syncId))
                restored.createdAt shouldBe createdAt
                val restoredLine = fresh.productPlanDao.getRows(restored.id).single()
                restoredLine.syncId shouldBe line.syncId
                fresh.productDao.getProduct(restoredLine.productId).maxMixGrams shouldBe 9000L
                MirrorDeletionJournalRepository(db).captureHardDeletes { db.productPlanDao.deletePlansByIds(setOf(id)) }
                val afterDelete = MirrorLocalSnapshotRepository(db).loadSnapshot(tables)
                apply.apply(afterDelete.rowsByTable.flatMap { (table, rows) -> rows.map { MirrorPushEntityChange(table, it) } })
                fresh.productPlanDao.getPlanBySyncId(syncId) shouldBe null
                fresh.productPlanDao.getRows(restored.id) shouldBe emptyList()
            }
        }
    }

    test("the replacement PDF prints the date and page count and continues tall rows without repeating weights") {
        withPdfFolder { folder ->
            val cardamom = RecipeIngredient("Кардамон натуральный ароматизатор жидкий, 40400828", 9)
            val oversized = RecipeIngredient("${"Продолжение ".repeat(400)}Конец длинной строки", 1)
            val recipe = listOf(cardamom, oversized) + List(99) { RecipeIngredient("Ингредиент №${it + 1}", 10) }
            val product = pdfProduct("КПД Реогард", 21_000, 18_000, 3_000, recipe)
            val first = ProductPlanLayout(LocalDateTime(2026, 1, 1, 8, 7, 6), listOf(product))
            val second = first.copy(createdAt = LocalDateTime(2026, 1, 2, 9, 8, 7))
            val path = replaceProductPlanPdf(first, folder)
            replaceProductPlanPdf(second, folder) shouldBe path
            Loader.loadPDF(path.toFile()).use { pdf ->
                (pdf.numberOfPages > 1) shouldBe true
                val pages = (1..pdf.numberOfPages).map { page ->
                    val box = pdf.getPage(page - 1).mediaBox
                    box.width shouldBe PDRectangle.A4.width
                    box.height shouldBe PDRectangle.A4.height
                    pdf.pageText(page).also { text ->
                        text.lineSequence().first() shouldBe "02.01.2026"
                        text.lineSequence().last() shouldBe "Стр. $page из ${pdf.numberOfPages}"
                        ("КПД Реогард" in text && "Замес 2" in text && "Ингредиент" in text) shouldBe true
                    }
                }
                val text = pages.joinToString("\n")
                ("01.01.2026" in text || "09:08:07" in text || "Навески" in text || "пак." in text) shouldBe false
                ("Ингредиент №99" in text) shouldBe true
                ("Конец длинной строки" in text.replace(Regex("""\s+"""), " ")) shouldBe true
                (pages.first().contains("Кардамон") && pages.first().contains("40400828")) shouldBe true
                Regex("""\bПродолжение\b""").findAll(text).count() shouldBe 400
                Regex("""\b0,012\b""").findAll(text).count() shouldBe 1
                Regex("""\b0,009\b""").findAll(text).count() shouldBe 1
            }
            Files.list(folder).use { it.count() shouldBe 1L }
        }
    }

    test("the v2 plan keeps the first three products together and moves Derevenskaya in full") {
        val carrageenan = "Каррагинан рафинированный (капа) E407a"
        val guar = "Гуаровая камедь GELEON 5000 (E412)"
        val maltodextrin = "Мальтодекстрин MultyDex 18"
        val pyrophosphate = "Пирофосфат натрия(E450iii)"
        val products = listOf(
            pdfProduct("КПД Эластен", 99_000, 27_000, 3_000, pdfRecipe(
                carrageenan to 104, guar to 250, "Конжаковая камедь(E425)" to 97, maltodextrin to 549,
            )),
            pdfProduct("КПД Реогард", 66_000, 24_000, 3_000, pdfRecipe(carrageenan to 200, maltodextrin to 800)),
            pdfProduct("КПД Гельмикс Про", 30_000, 15_000, 3_000, pdfRecipe(
                "Сульфат кальция(E516)" to 120, maltodextrin to 544, guar to 80,
                pyrophosphate to 36, "Альгинат натрия" to 220,
            )),
            pdfProduct("КПД Деревенская", 60_000, 21_000, 3_000, pdfRecipe(
                "Чеснок" to 130, pyrophosphate to 200, maltodextrin to 97,
                "Диоксид кремния(E551)" to 3, guar to 120,
                "Ароматизатор натуральный «Перец черный 11.01.333» DEL'AR" to 25,
                "Триполифосфат натрия(E451i)" to 100,
                "Кардамон натуральный ароматизатор жидкий, 40400828" to 25, "Соль" to 300,
            )),
        )
        withPdfFolder { folder ->
            val layout = ProductPlanLayout(LocalDateTime(2026, 11, 22, 9, 8, 7), products)
            val path = replaceProductPlanPdf(layout, folder)
            Loader.loadPDF(path.toFile()).use { pdf ->
                pdf.numberOfPages shouldBe 2
                val first = pdf.pageText(1)
                val second = pdf.pageText(2)
                products.take(3).all { it.name in first && it.name !in second } shouldBe true
                ("КПД Деревенская" in first || "Чеснок" in first) shouldBe false
                ("КПД Деревенская" in second && "Чеснок" in second && "Соль" in second) shouldBe true
            }
        }
    }

    test("six batches retain numbering and printed weights sum to each printed mass") {
        val recipe = pdfRecipe("Альфа" to 333, "Бета" to 333, "Гамма" to 334)
        val splitCases = listOf(
            Triple(7_003L, 1_201L, listOf(4, 2)),
            Triple(Long.MAX_VALUE, 1_600_000_000_000_000_000L, listOf(1, 1, 1, 1, 1, 1)),
        )
        val createdAt = LocalDateTime(2026, 11, 22, 9, 8, 7)
        val headers = Regex("""Замес \d+\s+([\d,]+) кг""")
        withPdfFolder { folder ->
            for ((total, capacity, groupSizes) in splitCases) {
                val product = pdfProduct("Тест", total, capacity, 1, recipe)
                val path = replaceProductPlanPdf(ProductPlanLayout(createdAt, listOf(product)), folder)
                Loader.loadPDF(path.toFile()).use { pdf ->
                    val text = PDFTextStripper().getText(pdf)
                    Regex("""Замес (\d+)""").findAll(text).map { it.groupValues[1].toInt() }.toList() shouldBe
                        (1..6).toList()
                    val tables = text.split("Ингредиент").drop(1)
                    tables.map { headers.findAll(it).count() } shouldBe groupSizes
                    val printedMasses = mutableListOf<Long>()
                    tables.forEach { table ->
                        val batchGrams = headers.findAll(table).map { requireNotNull(parseGrams(it.groupValues[1])) }.toList()
                        val rows = recipe.map { ingredient ->
                            val line = table.lineSequence().single { it.startsWith("${ingredient.name} ") }
                            line.substringAfter(' ').trim().split(Regex("""\s+""")).map { requireNotNull(parseGrams(it)) }
                        }
                        rows.forEach { it.size shouldBe batchGrams.size }
                        batchGrams.indices.map { column -> rows.sumOf { it[column] } } shouldBe batchGrams
                        printedMasses += batchGrams
                    }
                    printedMasses shouldBe (0L..5L).map { product.batches.packagesAt(it) * product.packageGrams }
                }
            }
        }
    }
})

private fun pdfProduct(name: String, grams: Long, capacity: Long, packageGrams: Long, recipe: List<RecipeIngredient>) =
    ProductLayout(name, grams, capacity, packageGrams, splitBatches(grams, capacity, packageGrams), recipe)

private fun pdfRecipe(vararg entries: Pair<String, Long>) = entries.map { (name, grams) -> RecipeIngredient(name, grams) }

private fun PDDocument.pageText(page: Int): String = PDFTextStripper().apply {
    startPage = page
    endPage = page
}.getText(this).trim()

private inline fun withPdfFolder(block: (Path) -> Unit) {
    val folder = Files.createTempDirectory("nocombro-production-test")
    try {
        block(folder)
    } finally {
        Files.list(folder).use { paths -> paths.forEach { Files.deleteIfExists(it) } }
        Files.deleteIfExists(folder)
    }
}
