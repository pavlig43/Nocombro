package ru.pavlig43.database

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.BatchBD
import ru.pavlig43.database.data.batch.BatchMovement
import ru.pavlig43.database.data.batch.MovementType
import ru.pavlig43.database.data.storage.StorageBatch
import ru.pavlig43.database.data.storage.StorageProduct
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.scenario

class StorageSmokeTest : DesktopMainDispatcherFunSpec({

    val marchStart = LocalDateTime(2026, 3, 1, 0, 0)
    val marchEnd = LocalDateTime(2026, 3, 31, 23, 59, 59)

    test(
        scenario(
            given = "the seeded database",
            whenAction = "storage is calculated for March 2026",
            thenResult = "product totals stay stable and match the seed movements",
        )
    ) {
        withSeededTestDatabase { db ->
            val storage = db.storageDao.observeOnStorageProduct(
                start = marchStart,
                end = marchEnd,
            ).first()

            storage.shouldHaveSize(5)

            storage.first { it.productName == "Соль" }.apply {
                balanceBeforeStart shouldBe 0L
                incoming shouldBe 80000L
                outgoing shouldBe 24300L
                balanceOnEnd shouldBe 55700L
                batches.shouldHaveSize(2)
            }

            storage.first { it.productName == "Декстроза" }.apply {
                balanceBeforeStart shouldBe 50000L
                incoming shouldBe 100000L
                outgoing shouldBe 24300L
                balanceOnEnd shouldBe 125700L
                batches.shouldHaveSize(2)
            }

            storage.first { it.productName == "Колбаски Баварские" }.apply {
                balanceBeforeStart shouldBe 0L
                incoming shouldBe 54000L
                outgoing shouldBe 66000L
                balanceOnEnd shouldBe -12000L
                batches.shouldHaveSize(2)
            }
        }
    }

    test(
        scenario(
            given = "the seeded database",
            whenAction = "batch movement balances are calculated",
            thenResult = "running balances stay internally consistent",
        )
    ) {
        withSeededTestDatabase { db ->
            val batchInfo = db.storageDao.observeBatchMovementsWithBalance(
                batchId = 1,
                start = marchStart,
                end = marchEnd,
            ).first()

            batchInfo.productName shouldBe "Соль"
            batchInfo.movements.shouldHaveSize(3)

            batchInfo.movements[0].apply {
                balanceBeforeStart shouldBe 0L
                incoming shouldBe 30000L
                outgoing shouldBe 0L
                balanceOnEnd shouldBe 30000L
                transactionId shouldBe 1
            }

            batchInfo.movements[1].apply {
                balanceBeforeStart shouldBe 30000L
                incoming shouldBe 0L
                outgoing shouldBe 16200L
                balanceOnEnd shouldBe 13800L
                transactionId shouldBe 9
            }

            batchInfo.movements[2].apply {
                balanceBeforeStart shouldBe 13800L
                incoming shouldBe 0L
                outgoing shouldBe 8100L
                balanceOnEnd shouldBe 5700L
                transactionId shouldBe 10
            }
        }
    }

    test(
        scenario(
            given = "batches with ordered and invalid movement histories",
            whenAction = "storage is calculated for different period boundaries",
            thenResult = "negative history depends on the period end but not its start",
        )
    ) {
        withSeededTestDatabase { db ->
            val repairedBatchId = db.createStorageTestBatch(
                StorageMovementSeed(
                    createdAt = LocalDateTime(2027, 1, 2, 10, 0),
                    movementType = MovementType.OUTGOING,
                    count = 1000L,
                ),
                StorageMovementSeed(
                    createdAt = LocalDateTime(2027, 1, 3, 10, 0),
                    movementType = MovementType.INCOMING,
                    count = 1000L,
                ),
            )
            val healthyBatchId = db.createStorageTestBatch(
                StorageMovementSeed(
                    createdAt = LocalDateTime(2027, 1, 2, 11, 0),
                    movementType = MovementType.INCOMING,
                    count = 1000L,
                ),
                StorageMovementSeed(
                    createdAt = LocalDateTime(2027, 1, 3, 11, 0),
                    movementType = MovementType.OUTGOING,
                    count = 1000L,
                ),
            )
            val outgoingOnlyBatchId = db.createStorageTestBatch(
                StorageMovementSeed(
                    createdAt = LocalDateTime(2027, 1, 2, 12, 0),
                    movementType = MovementType.OUTGOING,
                    count = 500L,
                ),
            )

            val beforeFirstMovement = db.storageDao.observeOnStorageProduct(
                start = LocalDateTime(2026, 12, 1, 0, 0),
                end = LocalDateTime(2027, 1, 1, 23, 59),
            ).first()
            beforeFirstMovement.findBatch(repairedBatchId) shouldBe null

            val whileNegative = db.storageDao.observeOnStorageProduct(
                start = LocalDateTime(2027, 1, 2, 0, 0),
                end = LocalDateTime(2027, 1, 2, 23, 59),
            ).first()
            requireNotNull(whileNegative.findBatch(repairedBatchId)).apply {
                outgoing shouldBe 1000L
                balanceOnEnd shouldBe -1000L
                hasNegativeBalanceHistory shouldBe true
            }

            val afterRepair = db.storageDao.observeOnStorageProduct(
                start = LocalDateTime(2027, 1, 4, 0, 0),
                end = LocalDateTime(2027, 1, 5, 23, 59),
            ).first()
            requireNotNull(afterRepair.findBatch(repairedBatchId)).apply {
                balanceBeforeStart shouldBe 0L
                incoming shouldBe 0L
                outgoing shouldBe 0L
                balanceOnEnd shouldBe 0L
                hasNegativeBalanceHistory shouldBe true
            }
            val healthyHistory = db.storageDao.observeOnStorageProduct(
                start = LocalDateTime(2027, 1, 2, 0, 0),
                end = LocalDateTime(2027, 1, 3, 23, 59),
            ).first()
            requireNotNull(healthyHistory.findBatch(healthyBatchId)).apply {
                balanceOnEnd shouldBe 0L
                hasNegativeBalanceHistory shouldBe false
            }
            requireNotNull(afterRepair.findBatch(outgoingOnlyBatchId)).apply {
                balanceOnEnd shouldBe -500L
                hasNegativeBalanceHistory shouldBe true
            }
        }
    }
})

private data class StorageMovementSeed(
    val createdAt: LocalDateTime,
    val movementType: MovementType,
    val count: Long,
)

private suspend fun NocombroDatabase.createStorageTestBatch(
    vararg movements: StorageMovementSeed,
): Int {
    val batchId = batchDao.createBatch(
        BatchBD(
            id = 0,
            productId = 1,
            dateBorn = LocalDate(2027, 1, 1),
            declarationId = 1,
        )
    ).toInt()

    movements.forEach { movement ->
        val transactionId = transactionDao.create(
            Transact(
                transactionType = when (movement.movementType) {
                    MovementType.INCOMING -> TransactionType.BUY
                    MovementType.OUTGOING -> TransactionType.OPZS
                },
                createdAt = movement.createdAt,
                comment = "",
                isCompleted = true,
            )
        ).toInt()
        batchMovementDao.createMovement(
            BatchMovement(
                batchId = batchId,
                movementType = movement.movementType,
                count = movement.count,
                transactionId = transactionId,
            )
        )
    }

    return batchId
}

private fun List<StorageProduct>.findBatch(batchId: Int): StorageBatch? {
    return asSequence()
        .flatMap { it.batches.asSequence() }
        .firstOrNull { it.batchId == batchId }
}
