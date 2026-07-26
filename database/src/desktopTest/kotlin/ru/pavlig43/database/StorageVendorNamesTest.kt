package ru.pavlig43.database

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.scenario

class StorageVendorNamesTest : DesktopMainDispatcherFunSpec({
    test(
        scenario(
            given = "seeded storage batches",
            whenAction = "storage rows are calculated",
            thenResult = "product and batch rows expose their suppliers without changing balances",
        )
    ) {
        withSeededTestDatabase { db ->
            val storage = db.storageDao.observeOnStorageProduct(
                start = LocalDateTime(2026, 3, 1, 0, 0),
                end = LocalDateTime(2026, 3, 31, 23, 59, 59),
            ).first()

            storage.first { it.productName == "Соль" }.apply {
                vendorNames shouldBe "Стоинг"
                batches.all { it.vendorName == "Стоинг" } shouldBe true
                balanceOnEnd shouldBe 55_700L
            }
        }
    }
})
