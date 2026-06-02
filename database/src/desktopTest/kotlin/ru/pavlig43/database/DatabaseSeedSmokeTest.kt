package ru.pavlig43.database

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withEmptyTestDatabase
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.scenario

class DatabaseSeedSmokeTest : DesktopMainDispatcherFunSpec({

    test(
        scenario(
            given = "a fresh empty test database",
            whenAction = "core collections are queried",
            thenResult = "they are empty and the schema boots cleanly",
        )
    ) {
        withEmptyTestDatabase { db ->
            db.vendorDao.observeOnVendors().first().shouldBeEmpty()
            db.productDao.observeOnProducts().first().shouldBeEmpty()
            db.declarationDao.observeOnItems().first().shouldBeEmpty()
            db.transactionDao.observeOnProductTransactions().first().shouldBeEmpty()
            db.batchDao.observeAllMovements().first().shouldBeEmpty()
            db.expenseDao.getAll().shouldBeEmpty()
            db.reminderDao.observeAllReminders().first().shouldBeEmpty()
        }
    }

    test(
        scenario(
            given = "the built-in seeded database",
            whenAction = "core tables are queried",
            thenResult = "the expected baseline counts are present",
        )
    ) {
        withSeededTestDatabase { db ->
            db.vendorDao.observeOnVendors().first().size shouldBe 7
            db.productDao.observeOnProducts().first().size shouldBe 5
            db.declarationDao.observeOnItems().first().size shouldBe 6
            db.transactionDao.observeOnProductTransactions().first().size shouldBe 11
            db.batchDao.observeAllMovements().first().size shouldBe 22
            db.expenseDao.getAll().size shouldBe 1
            db.reminderDao.observeAllReminders().first().size shouldBe 1
            db.batchCostDao.getBatchesCostPriceByIds((1..10).toList()).size shouldBe 10
        }
    }
})
