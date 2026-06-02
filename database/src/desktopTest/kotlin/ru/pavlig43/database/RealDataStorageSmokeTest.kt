package ru.pavlig43.database

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import kotlin.io.path.Path

private const val REAL_DATA_STORAGE_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataStorageSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_STORAGE_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    val wideStart = LocalDateTime(2000, 1, 1, 0, 0)
    val wideEnd = LocalDateTime(2100, 1, 1, 0, 0)

    test(
        scenario(
            given = "a real copied database dump",
            whenAction = "storage aggregates are built on a wide period",
            thenResult = "product totals match batch totals and movement balances stay coherent",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath.shouldNotBeNull()) { db ->
            val storage = db.storageDao.observeOnStorageProduct(
                start = wideStart,
                end = wideEnd,
            ).first()

            storage.shouldNotBeEmpty()

            storage.take(10).forEach { product ->
                product.productName.isNotBlank().shouldBeTrue()
                product.balanceOnEnd shouldBe product.balanceBeforeStart + product.incoming - product.outgoing
                product.balanceBeforeStart shouldBe product.batches.sumOf { it.balanceBeforeStart }
                product.incoming shouldBe product.batches.sumOf { it.incoming }
                product.outgoing shouldBe product.batches.sumOf { it.outgoing }
                product.balanceOnEnd shouldBe product.batches.sumOf { it.balanceOnEnd }

                product.batches.forEach { batch ->
                    batch.batchName.isNotBlank().shouldBeTrue()
                    batch.balanceOnEnd shouldBe batch.balanceBeforeStart + batch.incoming - batch.outgoing
                }
            }

            val sampleBatchId = storage
                .firstOrNull { it.batches.isNotEmpty() }
                ?.batches
                ?.firstOrNull()
                ?.batchId
                .shouldNotBeNull()

            val batchInfo = db.storageDao.observeBatchMovementsWithBalance(
                batchId = sampleBatchId,
                start = wideStart,
                end = wideEnd,
            ).first()

            batchInfo.movements.shouldNotBeEmpty()
            batchInfo.movements.forEach { movement ->
                movement.balanceOnEnd shouldBe movement.balanceBeforeStart + movement.incoming - movement.outgoing
                (movement.incoming == 0L || movement.outgoing == 0L).shouldBeTrue()
            }
        }
    }
})
