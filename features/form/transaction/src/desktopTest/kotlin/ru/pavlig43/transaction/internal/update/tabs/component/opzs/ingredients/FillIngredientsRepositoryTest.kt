package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.ingredient.IngredientBD
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf.PfUi

class FillIngredientsRepositoryTest : DesktopMainDispatcherFunSpec({

    test("one batch covers the whole required weight") {
        val result = allocateIngredients(
            transactionId = 50,
            requiredIngredients = listOf(requiredIngredient(1, "Мука", 3_500)),
            availableBatches = listOf(availableBatch(10, 1, "Мука", 8_000)),
        ).shouldBeInstanceOf<FillIngredientsResult.Ready>()

        result.ingredients.map { it.batchId to it.count } shouldContainExactly listOf(10 to 3_500L)
    }

    test("weight is split by date and then batch id while product rows stay together") {
        val result = allocateIngredients(
            transactionId = 50,
            requiredIngredients = listOf(
                requiredIngredient(1, "Мука", 1_500),
                requiredIngredient(2, "Соль", 500),
            ),
            availableBatches = listOf(
                availableBatch(30, 1, "Мука", 5_000, LocalDate(2026, 2, 1)),
                availableBatch(20, 1, "Мука", 1_000, LocalDate(2026, 1, 1)),
                availableBatch(5, 2, "Соль", 500, LocalDate(2025, 1, 1)),
                availableBatch(10, 1, "Мука", 1_000, LocalDate(2026, 1, 1)),
            ),
        ).shouldBeInstanceOf<FillIngredientsResult.Ready>()

        result.ingredients.map { Triple(it.productId, it.batchId, it.count) } shouldContainExactly listOf(
            Triple(1, 10, 1_000L),
            Triple(1, 20, 500L),
            Triple(2, 5, 500L),
        )
    }

    test("all shortages are returned instead of partial rows") {
        val result = allocateIngredients(
            transactionId = 50,
            requiredIngredients = listOf(
                requiredIngredient(1, "Мука", 3_500),
                requiredIngredient(2, "Соль", 2_000),
            ),
            availableBatches = listOf(
                availableBatch(10, 1, "Мука", 2_000),
                availableBatch(20, 2, "Соль", 500),
            ),
        ).shouldBeInstanceOf<FillIngredientsResult.Deficit>()

        result.deficits shouldContainExactly listOf(
            IngredientDeficit(productId = 1, productName = "Мука", missingCount = 1_500),
            IngredientDeficit(productId = 2, productName = "Соль", missingCount = 1_500),
        )
    }

    test("editing OPZS ignores its old outgoing movements") {
        withSeededTestDatabase { db ->
            val result = FillIngredientsRepository(db).fillIngredientsFromComposition(
                productId = 3,
                transactionId = 10,
                countPf = 18_000,
            ).getOrThrow().shouldBeInstanceOf<FillIngredientsResult.Ready>()

            result.ingredients.map { Triple(it.productId, it.batchId, it.count) } shouldContainExactly listOf(
                Triple(1, 1, 8_100L),
                Triple(2, 8, 8_100L),
                Triple(4, 3, 1_800L),
                Triple(5, 5, 6_000L),
            )
        }
    }

    test("only active main-storage batches and movements with positive balance are used") {
        withSeededTestDatabase { db ->
            val ingredientId = db.productDao.create(
                Product(
                    type = ProductType.FOOD_BASE,
                    displayName = "Тестовое сырьё",
                    createdAt = LocalDate(2026, 1, 1),
                )
            ).toInt()
            val pfId = db.productDao.create(
                Product(
                    type = ProductType.FOOD_PF,
                    displayName = "Тестовый ПФ",
                    createdAt = LocalDate(2026, 1, 1),
                )
            ).toInt()
            db.compositionDao.upsertComposition(
                listOf(
                    CompositionIn(
                        id = 0,
                        parentId = pfId,
                        productId = ingredientId,
                        count = 1_000,
                    )
                )
            )

            suspend fun createBatch(
                dateBorn: LocalDate,
                deletedAt: LocalDateTime? = null,
            ): Int = db.batchDao.createBatch(
                BatchBD(
                    id = 0,
                    productId = ingredientId,
                    dateBorn = dateBorn,
                    declarationId = 1,
                    deletedAt = deletedAt,
                )
            ).toInt()

            suspend fun addMovement(
                batchId: Int,
                movementType: MovementType,
                count: Long,
                transactionId: Int = 1,
                storageLocation: StorageLocation = StorageLocation.MAIN,
                deletedAt: LocalDateTime? = null,
            ) {
                db.batchMovementDao.createMovement(
                    BatchMovement(
                        batchId = batchId,
                        movementType = movementType,
                        count = count,
                        transactionId = transactionId,
                        storageLocation = storageLocation,
                        deletedAt = deletedAt,
                    )
                )
            }

            createBatch(LocalDate(2021, 1, 1))
            val deletedMovementBatch = createBatch(LocalDate(2022, 1, 1))
            val zeroBalanceBatch = createBatch(LocalDate(2023, 1, 1))
            val experimentalBatch = createBatch(LocalDate(2024, 1, 1))
            val deletedBatch = createBatch(
                dateBorn = LocalDate(2025, 1, 1),
                deletedAt = LocalDateTime(2026, 1, 1, 0, 0),
            )
            val validNewerBatch = createBatch(LocalDate(2027, 1, 1))

            addMovement(
                batchId = deletedMovementBatch,
                movementType = MovementType.INCOMING,
                count = 1_000,
                deletedAt = LocalDateTime(2026, 1, 1, 0, 0),
            )
            addMovement(zeroBalanceBatch, MovementType.INCOMING, 1_000)
            addMovement(zeroBalanceBatch, MovementType.OUTGOING, 1_000, transactionId = 9)
            addMovement(
                batchId = experimentalBatch,
                movementType = MovementType.INCOMING,
                count = 1_000,
                storageLocation = StorageLocation.EXPERIMENTAL,
            )
            addMovement(deletedBatch, MovementType.INCOMING, 1_000)
            addMovement(validNewerBatch, MovementType.INCOMING, 1_000)

            val result = FillIngredientsRepository(db).fillIngredientsFromComposition(
                productId = pfId,
                transactionId = 10,
                countPf = 1_000,
            ).getOrThrow().shouldBeInstanceOf<FillIngredientsResult.Ready>()

            result.ingredients.single().run {
                batchId shouldBe validNewerBatch
                count shouldBe 1_000L
                dateBorn shouldBe LocalDate(2027, 1, 1)
            }
        }
    }

    test("a shortage leaves table rows unchanged and blocks saving until PF weight changes") {
        withSeededTestDatabase { db ->
            val initialIngredient = requiredIngredient(1, "Исходная строка", 777).copy(
                transactionId = 10,
                batchId = 2,
                id = 15,
            )
            var savedIngredients: List<IngredientBD>? = null
            val pfFlow = MutableStateFlow(
                PfUi(
                    transactionId = 10,
                    productId = 3,
                    declarationId = 5,
                    count = DecimalData3(1_000_000),
                )
            )
            val component = runOnUiThread {
                IngredientComponent(
                    componentComponent = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    transactionId = 10,
                    tabOpener = NoopTabOpener,
                    pfFlow = pfFlow,
                    immutableTableDependencies = ImmutableTableDependencies(db),
                    repository = object : UpdateCollectionRepository<IngredientBD, IngredientBD> {
                        override suspend fun getInit(id: Int): Result<List<IngredientBD>> =
                            Result.success(listOf(initialIngredient))

                        override suspend fun update(
                            changeSet: ChangeSet<List<IngredientBD>>,
                        ): Result<Unit> {
                            savedIngredients = changeSet.new
                            return Result.success(Unit)
                        }
                    },
                    fillIngredientsRepository = FillIngredientsRepository(db),
                )
            }
            waitUntil { component.itemList.value.size == 1 }

            runOnUiThread(component::fillFromPf)
            waitUntil {
                (component.loadCompositionState.value as? LoadCompositionState.Deficit)
                    ?.messages?.size == 3
            }

            component.itemList.value.single().run {
                id shouldBe initialIngredient.id
                batchId shouldBe initialIngredient.batchId
                balance.value shouldBe initialIngredient.count
            }
            val deficitMessages =
                (component.loadCompositionState.value as LoadCompositionState.Deficit).messages
            deficitMessages shouldContain
                "Сырьё «Соль»: не хватает 386,200 кг"
            component.errorMessages.first { errors ->
                errors.any { it.startsWith("Сырьё «Соль»") }
            }.any { it.startsWith("Сырьё «Соль»") } shouldBe true

            pfFlow.value = pfFlow.value.copy(declarationId = 1)
            (component.loadCompositionState.value as LoadCompositionState.Deficit).messages.size shouldBe 3

            pfFlow.value = pfFlow.value.copy(count = DecimalData3(18_000))
            component.onPfProductOrCountChanged()
            component.loadCompositionState.value shouldBe LoadCompositionState.Success

            runOnUiThread(component::fillFromPf)
            waitUntil { component.itemList.value.size == 4 }
            component.loadCompositionState.value shouldBe LoadCompositionState.Success
            component.itemList.value.map { Triple(it.productId, it.batchId, it.balance.value) } shouldContainExactly listOf(
                Triple(1, 1, 8_100L),
                Triple(2, 8, 8_100L),
                Triple(4, 3, 1_800L),
                Triple(5, 5, 6_000L),
            )

            component.onUpdate().getOrThrow()
            savedIngredients.orEmpty().map { Triple(it.productId, it.batchId, it.count) } shouldContainExactly
                component.itemList.value.map { Triple(it.productId, it.batchId, it.balance.value) }
        }
    }

    test("duplicate batches in saved OPZS rows mark every duplicate row invalid") {
        withSeededTestDatabase { db ->
            val rows = listOf(
                requiredIngredient(1, "Мука", 1_000).copy(id = 1, batchId = 7),
                requiredIngredient(1, "Мука", 2_000).copy(id = 2, batchId = 7),
            )
            val component = runOnUiThread {
                IngredientComponent(
                    componentComponent = DefaultComponentContext(LifecycleRegistry()),
                    transactionId = 10,
                    tabOpener = NoopTabOpener,
                    pfFlow = MutableStateFlow(PfUi()),
                    immutableTableDependencies = ImmutableTableDependencies(db),
                    repository = object : UpdateCollectionRepository<IngredientBD, IngredientBD> {
                        override suspend fun getInit(id: Int): Result<List<IngredientBD>> =
                            Result.success(rows)

                        override suspend fun update(
                            changeSet: ChangeSet<List<IngredientBD>>,
                        ): Result<Unit> = Result.success(Unit)
                    },
                    fillIngredientsRepository = FillIngredientsRepository(db),
                )
            }
            waitUntil { component.itemList.value.size == 2 }

            component.errorMessages.first { errors ->
                errors.count { it.endsWith("партия уже указана") } == 2
            }.filter { it.endsWith("партия уже указана") } shouldContainExactly listOf(
                "В строке 1 партия уже указана",
                "В строке 2 партия уже указана",
            )
        }
    }
})

private fun requiredIngredient(
    productId: Int,
    productName: String,
    count: Long,
): IngredientBD = IngredientBD(
    count = count,
    productType = ProductType.FOOD_BASE,
    productId = productId,
    productName = productName,
)

private fun availableBatch(
    batchId: Int,
    productId: Int,
    productName: String,
    balance: Long,
    dateBorn: LocalDate = LocalDate(2026, 1, 1),
): AvailableIngredientBatch = AvailableIngredientBatch(
    batchId = batchId,
    productId = productId,
    productName = productName,
    productType = ProductType.FOOD_BASE,
    vendorName = "Поставщик",
    dateBorn = dateBorn,
    balance = balance,
)
