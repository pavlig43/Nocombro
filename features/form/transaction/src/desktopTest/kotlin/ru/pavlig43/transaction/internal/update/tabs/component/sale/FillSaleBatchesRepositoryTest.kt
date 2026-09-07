package ru.pavlig43.transaction.internal.update.tabs.component.sale

import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.sale.SaleBDOut
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.component.MutableUiEvent
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.tablecore.manger.SelectionUiEvent
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.NoopTabOpener
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.runOnUiThread
import ru.pavlig43.testkit.waitUntil

class FillSaleBatchesRepositoryTest : DesktopMainDispatcherFunSpec({

    test("one batch replaces a manually selected batch and covers the whole weight") {
        val source = sale(count = 3_500, batchId = 99, batchSyncId = "old-batch")

        val result = allocateSalesByBatches(
            sales = listOf(source),
            availableBatches = listOf(availableBatch(batchId = 10, balance = 8_000)),
        ).shouldBeInstanceOf<FillSaleBatchesResult.Ready>()

        result.sales.single().run {
            batchId shouldBe 10
            batchSyncId shouldBe "batch-10"
            count shouldBe 3_500
            id shouldBe source.id
            syncId shouldBe source.syncId
            movementId shouldBe source.movementId
            movementSyncId shouldBe source.movementSyncId
        }
    }

    test("weight is split by birth date and batch id while row data and identities are preserved") {
        val source = sale(
            count = 2_500,
            price = 42_500,
            ndsPercent = 10,
            clientId = 7,
            clientName = "Клиент",
            comment = "Срочно",
        )

        val result = allocateSalesByBatches(
            sales = listOf(source),
            availableBatches = listOf(
                availableBatch(30, 1_000, LocalDate(2026, 2, 1)),
                availableBatch(20, 1_000, LocalDate(2026, 1, 1)),
                availableBatch(10, 1_000, LocalDate(2026, 1, 1)),
            ),
        ).shouldBeInstanceOf<FillSaleBatchesResult.Ready>()

        result.sales.map { it.batchId to it.count } shouldContainExactly listOf(
            10 to 1_000L,
            20 to 1_000L,
            30 to 500L,
        )
        result.sales.forEach { part ->
            part.productId shouldBe source.productId
            part.productName shouldBe source.productName
            part.price shouldBe source.price
            part.ndsPercent shouldBe source.ndsPercent
            part.clientId shouldBe source.clientId
            part.clientName shouldBe source.clientName
            part.comment shouldBe source.comment
        }
        result.sales.first().run {
            id shouldBe source.id
            syncId shouldBe source.syncId
            movementId shouldBe source.movementId
            movementSyncId shouldBe source.movementSyncId
        }
        result.sales.drop(1).forEach { part ->
            part.id shouldBe 0
            part.movementId shouldBe 0
            part.syncId shouldNotBe source.syncId
            part.movementSyncId shouldNotBe source.movementSyncId
        }
        result.sales[1].syncId shouldNotBe result.sales[2].syncId
        result.sales[1].movementSyncId shouldNotBe result.sales[2].movementSyncId
    }

    test("rows of one product share batch balances without spending them twice") {
        val result = allocateSalesByBatches(
            sales = listOf(
                sale(count = 700, id = 1, syncId = "sale-1"),
                sale(count = 600, id = 2, syncId = "sale-2"),
            ),
            availableBatches = listOf(
                availableBatch(10, 1_000, LocalDate(2026, 1, 1)),
                availableBatch(20, 1_000, LocalDate(2026, 2, 1)),
            ),
        ).shouldBeInstanceOf<FillSaleBatchesResult.Ready>()

        result.sales.map { Triple(it.syncId, it.batchId, it.count) } shouldContainExactly listOf(
            Triple("sale-1", 10, 700L),
            Triple("sale-2", 10, 300L),
            Triple(result.sales.last().syncId, 20, 300L),
        )
        result.sales.sumOf { it.count } shouldBe 1_300
    }

    test("shortages of one product are summed and no partial rows are returned") {
        val result = allocateSalesByBatches(
            sales = listOf(sale(count = 4_000), sale(count = 3_000)),
            availableBatches = listOf(availableBatch(10, 2_000)),
        ).shouldBeInstanceOf<FillSaleBatchesResult.Deficit>()

        result.deficits shouldContainExactly listOf(
            SaleBatchDeficit(
                productId = 1,
                productName = "Товар",
                missingCount = 5_000,
            ),
        )
    }

    test("repository ignores old sale movements and filters inactive stock") {
        withSeededTestDatabase { db ->
            val productId = db.productDao.create(
                Product(
                    type = ProductType.FOOD_BASE,
                    displayName = "FIFO товар",
                    createdAt = LocalDate(2026, 1, 1),
                )
            ).toInt()

            suspend fun createBatch(
                dateBorn: LocalDate,
                deletedAt: LocalDateTime? = null,
            ): Int = db.batchDao.createBatch(
                BatchBD(
                    id = 0,
                    productId = productId,
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

            val deletedMovementBatch = createBatch(LocalDate(2021, 1, 1))
            val experimentalBatch = createBatch(LocalDate(2022, 1, 1))
            val zeroBalanceBatch = createBatch(LocalDate(2023, 1, 1))
            val deletedBatch = createBatch(
                dateBorn = LocalDate(2024, 1, 1),
                deletedAt = LocalDateTime(2026, 1, 1, 0, 0),
            )
            val editedSaleBatch = createBatch(LocalDate(2025, 1, 1))

            addMovement(
                batchId = deletedMovementBatch,
                movementType = MovementType.INCOMING,
                count = 1_000,
                deletedAt = LocalDateTime(2026, 1, 1, 0, 0),
            )
            addMovement(
                batchId = experimentalBatch,
                movementType = MovementType.INCOMING,
                count = 1_000,
                storageLocation = StorageLocation.EXPERIMENTAL,
            )
            addMovement(zeroBalanceBatch, MovementType.INCOMING, 1_000)
            addMovement(zeroBalanceBatch, MovementType.OUTGOING, 1_000, transactionId = 9)
            addMovement(deletedBatch, MovementType.INCOMING, 1_000)
            addMovement(editedSaleBatch, MovementType.INCOMING, 1_000)
            addMovement(editedSaleBatch, MovementType.OUTGOING, 1_000, transactionId = 10)

            val result = DefaultFillSaleBatchesRepository(db).fill(
                transactionId = 10,
                sales = listOf(sale(productId = productId, count = 1_000, transactionId = 10)),
            ).getOrThrow().shouldBeInstanceOf<FillSaleBatchesResult.Ready>()

            result.sales.single().run {
                batchId shouldBe editedSaleBatch
                count shouldBe 1_000
                dateBorn shouldBe LocalDate(2025, 1, 1)
            }
        }
    }

    test("button states, atomic shortage and reset events are exposed by the component") {
        withSeededTestDatabase { db ->
            val initialSale = sale(count = 1_000)
            val fillStarted = CompletableDeferred<Unit>()
            val fillResult = CompletableDeferred<Result<FillSaleBatchesResult>>()
            val component = runOnUiThread {
                SaleComponent(
                    componentComponent = DefaultComponentContext(
                        lifecycle = LifecycleRegistry(),
                        backHandler = BackDispatcher(),
                    ),
                    transactionId = 10,
                    tabOpener = NoopTabOpener,
                    immutableTableDependencies = ImmutableTableDependencies(db),
                    repository = object : UpdateCollectionRepository<SaleBDOut, SaleBDOut> {
                        override suspend fun getInit(id: Int): Result<List<SaleBDOut>> =
                            Result.success(listOf(initialSale))

                        override suspend fun update(
                            changeSet: ChangeSet<List<SaleBDOut>>,
                        ): Result<Unit> = Result.success(Unit)
                    },
                    fillSaleBatchesRepository = object : FillSaleBatchesRepository {
                        override suspend fun fill(
                            transactionId: Int,
                            sales: List<SaleBDOut>,
                        ): Result<FillSaleBatchesResult> {
                            fillStarted.complete(Unit)
                            return fillResult.await()
                        }
                    },
                )
            }
            waitUntil { component.itemList.value.size == 1 && component.enabledFillButton.value }

            runOnUiThread(component::fillByBatches)
            fillStarted.await()
            waitUntil { component.fillBatchesState.value is FillSaleBatchesState.Loading }
            component.enabledFillButton.value.shouldBeFalse()
            fillResult.complete(
                Result.success(
                    FillSaleBatchesResult.Deficit(
                        listOf(SaleBatchDeficit(1, "Товар", 3_500))
                    )
                )
            )
            waitUntil { component.fillBatchesState.value is FillSaleBatchesState.Deficit }

            component.itemList.value.single().run {
                id shouldBe initialSale.id
                batchId shouldBe initialSale.batchId
                count.value shouldBe initialSale.count
            }
            component.errorMessages.first { errors ->
                "Товар «Товар»: не хватает 3,500 кг" in errors
            }

            val changedCount = component.itemList.value.single().copy(count = DecimalData3(2_000))
            runOnUiThread {
                component.onEvent(MutableUiEvent.UpdateItem(changedCount))
            }
            component.fillBatchesState.value shouldBe FillSaleBatchesState.Ready
            component.enabledFillButton.value.shouldBeTrue()

            runOnUiThread(component::fillByBatches)
            waitUntil { component.fillBatchesState.value is FillSaleBatchesState.Deficit }
            val changedProduct = component.itemList.value.single().copy(
                productId = 2,
                productName = "Другой товар",
            )
            runOnUiThread {
                component.onEvent(MutableUiEvent.UpdateItem(changedProduct))
            }
            component.fillBatchesState.value shouldBe FillSaleBatchesState.Ready
            runOnUiThread {
                component.onEvent(
                    MutableUiEvent.UpdateItem(
                        changedProduct.copy(productId = 1, productName = "Товар")
                    )
                )
            }

            runOnUiThread(component::fillByBatches)
            waitUntil { component.fillBatchesState.value is FillSaleBatchesState.Deficit }
            runOnUiThread {
                component.onEvent(MutableUiEvent.CreateNewItem)
            }
            component.fillBatchesState.value shouldBe FillSaleBatchesState.Ready
            component.enabledFillButton.value.shouldBeFalse()

            val newRow = component.itemList.value.last().copy(
                productId = 1,
                productName = "Товар",
                count = DecimalData3(1_000),
            )
            runOnUiThread {
                component.onEvent(MutableUiEvent.UpdateItem(newRow))
            }
            component.enabledFillButton.value.shouldBeTrue()

            runOnUiThread(component::fillByBatches)
            waitUntil { component.fillBatchesState.value is FillSaleBatchesState.Deficit }
            runOnUiThread {
                component.onEvent(
                    MutableUiEvent.Selection(
                        SelectionUiEvent.ToggleSelection(newRow.composeId)
                    )
                )
                component.onEvent(MutableUiEvent.DeleteSelected)
            }
            component.fillBatchesState.value shouldBe FillSaleBatchesState.Ready
            component.itemList.value.size shouldBe 1
            component.enabledFillButton.value.shouldBeTrue()
        }
    }
})

private fun sale(
    productId: Int = 1,
    count: Long,
    transactionId: Int = 10,
    batchId: Int = 5,
    batchSyncId: String = "batch-5",
    price: Long = 12_300,
    ndsPercent: Int = 20,
    clientId: Int = 3,
    clientName: String = "Клиент",
    comment: String = "Комментарий",
    id: Int = 11,
    syncId: String = "sale-11",
): SaleBDOut = SaleBDOut(
    transactionId = transactionId,
    count = count,
    batchId = batchId,
    batchSyncId = batchSyncId,
    movementId = 21,
    movementSyncId = "movement-$id",
    productId = productId,
    productName = "Товар",
    vendorName = "Старый поставщик",
    dateBorn = LocalDate(2026, 6, 1),
    clientName = clientName,
    clientId = clientId,
    price = price,
    comment = comment,
    ndsPercent = ndsPercent,
    syncId = syncId,
    id = id,
)

private fun availableBatch(
    batchId: Int,
    balance: Long,
    dateBorn: LocalDate = LocalDate(2026, 1, 1),
): AvailableSaleBatch = AvailableSaleBatch(
    batchId = batchId,
    batchSyncId = "batch-$batchId",
    productId = 1,
    vendorName = "Поставщик $batchId",
    dateBorn = dateBorn,
    balance = balance,
)
