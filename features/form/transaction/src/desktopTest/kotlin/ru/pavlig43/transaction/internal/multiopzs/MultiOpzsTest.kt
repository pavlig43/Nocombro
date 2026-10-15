package ru.pavlig43.transaction.internal.multiopzs

import androidx.room.useWriterConnection
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.koin.core.qualifier.qualifier
import org.koin.dsl.koinApplication
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.NocombroTransactionExecutor
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchCostPriceEntity
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.files.remote.NoopRemoteFileStorageGateway
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.database.data.transact.ingredient.IngredientBD
import ru.pavlig43.database.data.transact.pf.PfBD
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.transaction.internal.di.BatchCostRepository
import ru.pavlig43.transaction.internal.di.UpdateCollectionRepositoryType
import ru.pavlig43.transaction.internal.di.UpdateSingleLineRepositoryType
import ru.pavlig43.transaction.internal.di.createTransactionFormModule
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients.FillIngredientsRepository

private val productionTime = LocalDateTime(2026, 10, 14, 17, 35)
private val born = LocalDate(2026, 1, 1)

class MultiOpzsTest : DesktopMainDispatcherFunSpec({
    test("one OPZS stores product, gram weight, declaration, vendor, date, completion and cost") {
        withFixture { f ->
            val row = f.row(2_001)
            val id = f.service.create(row, productionTime)
            val transaction = f.db.transactionDao.getTransaction(id)
            transaction.transactionType shouldBe TransactionType.OPZS
            transaction.createdAt shouldBe productionTime
            transaction.isCompleted shouldBe true
            transaction.syncId shouldBe row.syncId
            transaction.deletedAt shouldBe null
            val pf = f.db.pfDao.getPf(id)!!
            pf.productId shouldBe f.product
            pf.count shouldBe 2_001L
            pf.declarationId shouldBe f.declaration
            pf.vendorName shouldBe "Поставщик теста"
            pf.dateBorn shouldBe productionTime.date
            val batch = f.db.batchDao.getBatch(pf.batchId)
            f.db.batchCostDao.getBySyncId(batch.syncId)!!.costPricePerUnit shouldBe 22_504L
            f.db.batchMovementDao.getByTransactionId(id).all {
                it.movement.syncId.isNotBlank() && it.movement.deletedAt == null
            } shouldBe true
        }
    }

    test("two OPZS consume shared stock in order and split the oldest batch first") {
        withFixture { f ->
            val first = f.service.create(f.row(2_000), productionTime)
            val second = f.service.create(f.row(2_000), productionTime)
            f.db.ingredientDao.getByTransactionId(first).sortedBy { it.movementId }.map { it.batchId to it.count } shouldBe
                listOf(f.oldBatch to 1_500L, f.newBatch to 500L)
            f.db.ingredientDao.getByTransactionId(second).map { it.batchId to it.count } shouldBe
                listOf(f.newBatch to 2_000L)
            f.db.batchMovementDao.getBalance(f.oldBatch, StorageLocation.MAIN) shouldBe 0L
            f.db.batchMovementDao.getBalance(f.newBatch, StorageLocation.MAIN) shouldBe 1_000L
        }
    }

    test("packaging is rounded up using the existing composition calculation") {
        withFixture { f ->
            val pack = f.db.productDao.create(Product(
                type = ProductType.PACK, displayName = "Пакет", createdAt = born,
            )).toInt()
            f.db.compositionDao.upsertComposition(listOf(CompositionIn(
                id = 0, parentId = f.product, productId = pack, count = 100,
            )))
            f.stock(pack, 10_000, born, 500)
            val id = f.service.create(f.row(1_501), productionTime)
            val ingredients = f.db.ingredientDao.getByTransactionId(id)
            ingredients.filter { it.productId == pack }.sumOf { it.count } shouldBe 1_000L
            ingredients.filter { it.productId == f.raw }.sumOf { it.count } shouldBe 1_501L
        }
    }

    test("shortage and empty composition leave no partial OPZS") {
        withFixture { f ->
            val before = f.snapshot()
            runCatching { f.service.create(f.row(5_001), productionTime) }.isFailure shouldBe true
            f.snapshot() shouldBe before
            val empty = f.db.productDao.create(Product(
                type = ProductType.FOOD_PF, displayName = "Без состава", createdAt = born,
            )).toInt()
            f.link(empty, f.declaration)
            runCatching { f.service.create(f.row(1_000).copy(productId = empty), productionTime) }
                .exceptionOrNull()!!.message shouldBe "Пустой состав: выпуск без сырья запрещён"
            f.snapshot() shouldBe before
        }
    }

    test("invalid product, weight and unrelated declaration are rejected before writing") {
        withFixture { f ->
            val unrelated = f.addDeclaration("Чужая")
            val before = f.snapshot()
            for (row in listOf(
                f.row(1_000).copy(productId = 0), f.row(1_000).copy(productId = 99999),
                f.row(0), f.row(-1), f.row(1_000).copy(declarationId = 0),
                f.row(1_000).copy(declarationId = unrelated),
            )) {
                runCatching { f.service.create(row, productionTime) }.isFailure shouldBe true
                f.snapshot() shouldBe before
            }
        }
    }

    for (stage in listOf("pf", "ingredients", "cost")) {
        test("failure after $stage write rolls back header, output, ingredients and cost") {
            withFixture { f ->
                val before = f.snapshot()
                if (stage == "cost") f.db.useWriterConnection { connection ->
                    connection.usePrepared("""
                        CREATE TRIGGER fail_cost AFTER INSERT ON batch_cost_price
                        BEGIN SELECT RAISE(ABORT, 'cost failed'); END
                    """.trimIndent()) { it.step() }
                }
                val service = f.failingService(stage)
                runCatching { service.create(f.row(1_000), productionTime) }.isFailure shouldBe true
                f.snapshot() shouldBe before
                f.db.batchMovementDao.getBalance(f.oldBatch, StorageLocation.MAIN) shouldBe 1_500L
                if (stage == "cost") f.db.useWriterConnection { connection ->
                    connection.usePrepared("DROP TRIGGER fail_cost") { it.step() }
                }
                (f.service.create(f.row(1_000), productionTime) > 0) shouldBe true
            }
        }
    }

    test("retry with the same sync id returns the saved OPZS without another withdrawal") {
        withFixture { f ->
            val row = f.row(2_000)
            val id = f.service.create(row, productionTime)
            val before = f.snapshot()
            f.service.create(row, productionTime) shouldBe id
            f.snapshot() shouldBe before
            f.db.batchMovementDao.getBalance(f.newBatch, StorageLocation.MAIN) shouldBe 3_000L
        }
    }

    test("one declaration is selected automatically; several require selection; product change clears vendor") {
        withFixture { f ->
            val c = f.component()
            val key = c.state.value.rows.single().syncId
            runOnUiThread { c.selectProduct(key, f.product, "Выпуск") }
            waitUntil { !c.state.value.rows.single().loading }
            c.state.value.rows.single().declarationId shouldBe f.declaration
            c.state.value.rows.single().vendorName shouldBe "Поставщик теста"
            val second = f.db.productDao.create(Product(
                type = ProductType.FOOD_PF, displayName = "Два варианта", createdAt = born,
            )).toInt()
            f.link(second, f.declaration)
            f.link(second, f.addDeclaration("Вторая"))
            runOnUiThread { c.selectProduct(key, second, "Два варианта") }
            waitUntil { !c.state.value.rows.single().loading }
            c.state.value.rows.single().declarationId shouldBe 0
            c.state.value.rows.single().vendorName shouldBe ""
            runOnUiThread { c.selectDeclaration(key, f.declaration, "Выбор", "Поставщик теста") }
            c.state.value.rows.single().declarationId shouldBe f.declaration
            runOnUiThread { c.selectProduct(key, f.raw, "Без деклараций") }
            waitUntil { !c.state.value.rows.single().loading }
            c.state.value.rows.single().declarationId shouldBe 0
            c.state.value.rows.single().vendorName shouldBe ""
        }
    }

    test("whole list is validated before any OPZS is created") {
        withFixture { f ->
            val c = f.component()
            f.fillRow(c, c.state.value.rows.single().syncId, 1_000)
            runOnUiThread { c.addRow(); c.create() }
            waitUntil { c.state.value.finished }
            c.state.value.createdCount shouldBe 0
            c.state.value.rows[1].result.shouldBeInstanceOf<OpzsRowResult.Failed>()
            f.db.transactionDao.getAll().count { it.transactionType == TransactionType.OPZS } shouldBe 0
        }
    }

    test("field errors are visible before creation and disappear as fields are corrected") {
        withFixture { f ->
            val c = f.component()
            val row = c.state.value.rows.single()
            c.state.value.validationErrors shouldBe listOf(
                "Строка 1: Выберите продукт",
                "Строка 1: Выберите декларацию",
                "Строка 1: Вес должен быть больше нуля",
            )
            f.fillRow(c, row.syncId, 1_000)
            c.state.value.errorMessages shouldBe emptyList()
            runOnUiThread {
                c.selectDeclaration(row.syncId, 0, "", "")
                c.changeCount(row.syncId, DecimalData3(0))
            }
            c.state.value.validationErrors shouldBe listOf(
                "Строка 1: Выберите декларацию",
                "Строка 1: Вес должен быть больше нуля",
            )
            runOnUiThread { c.removeRow(row.syncId) }
            c.state.value.errorMessages shouldBe emptyList()
        }
    }

    test("standard table keeps creation order and denies editing successful and running rows") {
        withFixture { f ->
            val c = f.component()
            val columns = multiOpzsColumns(c, running = false)
            columns.any { it.sortable || it.filter != null } shouldBe false
            columns.single { it.key == MultiOpzsField.VENDOR }.editable shouldBe false
            val count = columns.single { it.key == MultiOpzsField.COUNT }
            count.editable shouldBe true
            count.canStartEdit!!(f.row(1_000), 0) shouldBe true
            count.canStartEdit!!(f.row(1_000).copy(result = OpzsRowResult.Created(42)), 0) shouldBe false
            multiOpzsColumns(c, running = true).single { it.key == MultiOpzsField.COUNT }
                .canStartEdit!!(f.row(1_000), 0) shouldBe false
        }
    }

    test("middle failure preserves prior rows, locks successful edits and resumes without duplicates") {
        withFixture { f ->
            val c = f.component()
            runOnUiThread { c.addRow(); c.addRow() }
            val keys = c.state.value.rows.map { it.syncId }
            f.fillRow(c, keys[0], 2_000)
            f.fillRow(c, keys[1], 4_000)
            f.fillRow(c, keys[2], 1_000)
            runOnUiThread {
                c.changeDate(productionTime)
                c.create()
                c.create()
                c.addRow()
                c.removeRow(keys[2])
                c.changeCount(keys[0], DecimalData3(999))
                c.changeDate(LocalDateTime(2020, 1, 1, 0, 0))
                c.back()
            }
            waitUntil { c.state.value.finished }
            c.state.value.createdCount shouldBe 1
            c.state.value.rows.size shouldBe 3
            c.state.value.producedAt shouldBe productionTime
            c.state.value.rows[0].count.value shouldBe 2_000L
            c.state.value.rows[1].result.shouldBeInstanceOf<OpzsRowResult.Failed>()
            c.state.value.rows[2].result shouldBe OpzsRowResult.Waiting
            val first = c.state.value.rows[0]
            runOnUiThread {
                c.removeRow(keys[0]); c.changeCount(keys[0], DecimalData3(7))
                c.selectProduct(keys[0], f.raw, "Сырьё")
                c.selectDeclaration(keys[0], 0, "", "")
                c.changeCount(keys[1], DecimalData3(2_000)); c.create()
            }
            waitUntil { c.state.value.finished }
            c.state.value.createdCount shouldBe 3
            c.state.value.rows[0] shouldBe first
            f.db.transactionDao.getAll().count { it.transactionType == TransactionType.OPZS } shouldBe 3
            f.db.batchMovementDao.getBalance(f.newBatch, StorageLocation.MAIN) shouldBe 0L
            runOnUiThread { c.create() }
            f.db.transactionDao.getAll().count { it.transactionType == TransactionType.OPZS } shouldBe 3
        }
    }

    test("standard add and selection actions delete only unfinished rows") {
        withFixture { f ->
            val c = f.component()
            val first = c.state.value.rows.single()
            f.fillRow(c, first.syncId, 1_000)
            runOnUiThread { c.create() }
            waitUntil { c.state.value.finished }
            runOnUiThread {
                c.onTableEvent(MutableUiEvent.CreateNewItem)
                c.onTableEvent(MutableUiEvent.CreateNewItem)
                c.onTableEvent(MutableUiEvent.Selection(SelectionUiEvent.ToggleSelectAll(
                    c.state.value.rows.map { it.composeId }.toSet(),
                )))
            }
            c.state.value.selectedIds.size shouldBe 2
            (first.composeId in c.state.value.selectedIds) shouldBe false
            runOnUiThread { c.onTableEvent(MutableUiEvent.DeleteSelected) }
            c.state.value.rows.single().syncId shouldBe first.syncId
            c.state.value.rows.single().result.shouldBeInstanceOf<OpzsRowResult.Created>()
            c.state.value.selectedIds shouldBe emptySet()
            runOnUiThread {
                c.onTableEvent(MutableUiEvent.Selection(SelectionUiEvent.ToggleSelection(first.composeId)))
                c.onTableEvent(MutableUiEvent.DeleteSelected)
            }
            c.state.value.rows.size shouldBe 1
        }
    }

    test("multi form opens and returns within the create stack, without calling the tab opener") {
        withFixture { f ->
            // Every TabOpener method fails: even one automatic open makes the test fail.
            val opener = java.lang.reflect.Proxy.newProxyInstance(
                ru.pavlig43.core.tabs.TabOpener::class.java.classLoader,
                arrayOf(ru.pavlig43.core.tabs.TabOpener::class.java),
            ) { _, method, _ -> error("Unexpected tab open: ${method.name}") } as ru.pavlig43.core.tabs.TabOpener
            val form = runOnUiThread { TransactionFormComponent(
                0, opener, f.context(), f.dependencies,
            ) }
            runOnUiThread { form.openMultiOpzs() }
            form.model.value.title shouldBe "*МультиОПЗС"
            val multi = form.stack.value.active.instance.shouldBeInstanceOf<TransactionFormComponent.Child.MultiOpzs>().component
            form.stack.value.backStack.size shouldBe 1
            f.fillRow(multi, multi.state.value.rows.single().syncId, 1_000)
            runOnUiThread { multi.create() }
            waitUntil { multi.state.value.finished }
            multi.state.value.createdCount shouldBe 1
            form.stack.value.active.instance.shouldBeInstanceOf<TransactionFormComponent.Child.MultiOpzs>()
            runOnUiThread { multi.back() }
            form.stack.value.active.instance.shouldBeInstanceOf<TransactionFormComponent.Child.Create>()
        }
    }
})

private suspend fun withFixture(block: suspend (OpzsFixture) -> Unit) = withEmptyTestDatabase { db ->
    val fixture = OpzsFixture(db)
    try {
        fixture.seed()
        block(fixture)
    } finally {
        runOnUiThread { fixture.lifecycle.destroy() }
        fixture.koin.close()
    }
}

private class OpzsFixture(val db: NocombroDatabase) {
    val lifecycle = LifecycleRegistry()
    val dependencies = TransactionFormDependencies(
        db, NocombroTransactionExecutor(db), ImmutableTableDependencies(db),
        FilesDependencies(db = db, remoteFileStorageGateway = NoopRemoteFileStorageGateway()),
    )
    val koin = koinApplication { modules(createTransactionFormModule(dependencies)) }
    val service: CreateOpzsService get() = koin.koin.get()
    var product = 0
    var raw = 0
    var vendor = 0
    var declaration = 0
    var oldBatch = 0
    var newBatch = 0

    suspend fun seed() {
        vendor = db.vendorDao.create(Vendor("Поставщик теста")).toInt()
        declaration = addDeclaration("Декларация")
        raw = db.productDao.create(Product(type = ProductType.FOOD_BASE, displayName = "Сырьё", createdAt = born)).toInt()
        product = db.productDao.create(Product(type = ProductType.FOOD_PF, displayName = "Выпуск", createdAt = born)).toInt()
        link(product, declaration)
        db.compositionDao.upsertComposition(listOf(CompositionIn(0, product, raw, 1_000)))
        // Новая партия имеет меньший ID: проверяем именно сортировку по дате.
        newBatch = stock(raw, 3_500, LocalDate(2026, 2, 1), 30_000)
        oldBatch = stock(raw, 1_500, born, 20_000)
    }

    suspend fun addDeclaration(name: String): Int = db.declarationDao.create(Declaration(
        displayName = name, createdAt = born, vendorId = vendor, vendorName = "Поставщик теста",
        bornDate = born, bestBefore = LocalDate(2028, 4, 30), observeFromNotification = false,
    )).toInt()

    suspend fun link(productId: Int, declarationId: Int) = db.productDeclarationDao.upsertProductDeclarations(
        listOf(ProductDeclarationIn(productId, declarationId)),
    )

    suspend fun stock(productId: Int, count: Long, date: LocalDate, price: Long): Int {
        val transaction = db.transactionDao.create(Transact(TransactionType.BUY, productionTime, "", true)).toInt()
        val batch = BatchBD(0, productId, date, declaration)
        val id = db.batchDao.createBatch(batch).toInt()
        db.batchMovementDao.createMovement(BatchMovement(
            batchId = id, movementType = MovementType.INCOMING, count = count, transactionId = transaction,
        ))
        db.batchCostDao.upsert(listOf(BatchCostPriceEntity(id, price, batch.syncId)))
        return id
    }

    fun row(count: Long) = MultiOpzsRow(productId = product, declarationId = declaration, count = DecimalData3(count))
    fun context() = DefaultComponentContext(lifecycle = lifecycle, backHandler = BackDispatcher())
    suspend fun component() = runOnUiThread {
        MultiOpzsComponent(context(), service, dependencies.immutableTableDependencies, NoopTabOpener) {
            error("Back must be blocked during processing")
        }
    }

    suspend fun fillRow(component: MultiOpzsComponent, key: String, count: Long) {
        runOnUiThread { component.selectProduct(key, product, "Выпуск") }
        waitUntil { component.state.value.rows.first { it.syncId == key }.loading.not() }
        runOnUiThread { component.changeCount(key, DecimalData3(count)) }
    }

    suspend fun snapshot() = listOf(
        db.transactionDao.getAll().size, db.batchDao.getAll().size,
        db.batchMovementDao.getAll().size, db.batchCostDao.getAll().size,
    )

    fun failingService(stage: String): CreateOpzsService {
        val pf: UpdateSingleLineRepository<PfBD> = koin.koin.get(UpdateSingleLineRepositoryType.PF.qualifier)
        val ingredients: UpdateCollectionRepository<IngredientBD, IngredientBD> =
            koin.koin.get(UpdateCollectionRepositoryType.INGREDIENTS.qualifier)
        return CreateOpzsService(
            db, FillIngredientsRepository(db),
            object : UpdateSingleLineRepository<PfBD> by pf {
                override suspend fun update(changeSet: ChangeSet<PfBD>): Result<Unit> {
                    pf.update(changeSet).getOrThrow()
                    return if (stage == "pf") Result.failure(IllegalStateException("pf failed")) else Result.success(Unit)
                }
            },
            object : UpdateCollectionRepository<IngredientBD, IngredientBD> by ingredients {
                override suspend fun update(changeSet: ChangeSet<List<IngredientBD>>): Result<Unit> {
                    ingredients.update(changeSet).getOrThrow()
                    return if (stage == "ingredients") Result.failure(IllegalStateException("ingredients failed")) else Result.success(Unit)
                }
            },
            koin.koin.get<BatchCostRepository>(),
        )
    }
}
