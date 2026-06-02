package ru.pavlig43.database

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.scenario

class DatabaseSeedRelationsSmokeTest : DesktopMainDispatcherFunSpec({

    test(
        scenario(
            given = "the seeded reference dataset",
            whenAction = "important cross-entity links are resolved",
            thenResult = "vendor, declaration, sale and expense relations stay consistent",
        )
    ) {
        withSeededTestDatabase { db ->
            val declaration = db.declarationDao.getDeclaration(5)
            val vendor = db.vendorDao.getVendor(declaration.vendorId)
            val product = db.productDao.getProduct(3)
            val specification = db.productSpecificationDao.getByProductId(product.id)
            val sales = db.saleDao.getSalesWithDetails(transactionId = 11)
            val reminders = db.reminderDao.getByTransactionId(transactionId = 11)
            val expenses = db.expenseDao.getByTransactionId(transactionId = 11)

            vendor.displayName shouldBe declaration.vendorName
            vendor.displayName shouldBe "ИП Гармаш"
            product.displayName shouldBe "Колбаски Баварские"
            specification?.productId shouldBe product.id

            sales.shouldHaveSize(4)
            sales.all { it.clientName == "Высший Вкус" } shouldBe true
            sales.sumOf { it.count } shouldBe 66000L

            reminders.shouldHaveSize(1)
            reminders.single().text shouldBe "деньги"

            expenses.shouldHaveSize(1)
            expenses.single().amount shouldBe 125230L
        }
    }
})
