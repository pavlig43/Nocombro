package ru.pavlig43.database

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
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
})
