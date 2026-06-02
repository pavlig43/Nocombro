package ru.pavlig43.database

import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.flow.first
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import kotlin.io.path.Path

private const val REAL_DATA_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataDatabaseSmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    test(
        scenario(
            given = "a real copied database dump",
            whenAction = "high-level sanity checks are executed",
            thenResult = "key collections and cross-links look internally consistent",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath.shouldNotBeNull()) { db ->
            val vendors = db.vendorDao.observeOnVendors().first()
            val products = db.productDao.observeOnProducts().first()
            val declarations = db.declarationDao.observeOnItems().first()
            val transactions = db.transactionDao.observeOnProductTransactions().first()
            val reminders = db.reminderDao.observeAllReminders().first()
            val expenses = db.expenseDao.getAll()

            vendors.shouldNotBeEmpty()
            products.shouldNotBeEmpty()
            declarations.shouldNotBeEmpty()
            transactions.shouldNotBeEmpty()

            declarations.take(20).forEach { declaration ->
                val vendor = db.vendorDao.getVendor(declaration.vendorId)
                vendor.displayName shouldBe declaration.vendorName
                declaration.displayName.isNotBlank().shouldBeTrue()
            }

            products.take(20).forEach { product ->
                product.displayName.isNotBlank().shouldBeTrue()
            }

            reminders.take(20).forEach { reminder ->
                db.transactionDao.getTransaction(reminder.transactionId).id shouldBe reminder.transactionId
            }

            expenses
                .filter { it.transactionId != null }
                .take(20)
                .forEach { expense ->
                    db.transactionDao.getTransaction(expense.transactionId!!).id shouldBe expense.transactionId
                    expense.amount.shouldBeGreaterThan(0)
                }

            transactions
                .filter { it.transactionType == TransactionType.SALE }
                .take(10)
                .forEach { transaction ->
                    db.saleDao.getSalesWithDetails(transaction.id).forEach { sale ->
                        sale.clientName.isNotBlank().shouldBeTrue()
                        sale.productName.isNotBlank().shouldBeTrue()
                        sale.count.shouldBeGreaterThan(0)
                    }
                }
        }
    }

    test(
        scenario(
            given = "a real copied database dump",
            whenAction = "sample entities are reopened by id",
            thenResult = "core screens should have stable records to open tomorrow",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath.shouldNotBeNull()) { db ->
            val vendors = db.vendorDao.observeOnVendors().first()
            val products = db.productDao.observeOnProducts().first()
            val declarations = db.declarationDao.observeOnItems().first()
            val transactions = db.transactionDao.observeOnProductTransactions().first()
            val documents = db.documentDao.observeOnDocuments().first()
            val expenses = db.expenseDao.getAll()

            vendors.take(5).forEach { expected ->
                val actual = db.vendorDao.getVendor(expected.id)
                actual.id shouldBe expected.id
                actual.displayName.isNotBlank().shouldBeTrue()
            }

            products.take(5).forEach { expected ->
                val actual = db.productDao.getProduct(expected.id)
                actual.id shouldBe expected.id
                actual.displayName.isNotBlank().shouldBeTrue()
            }

            declarations.take(5).forEach { expected ->
                val actual = db.declarationDao.getDeclaration(expected.id)
                actual.id shouldBe expected.id
                actual.displayName.isNotBlank().shouldBeTrue()
                actual.vendorName.isNotBlank().shouldBeTrue()
            }

            transactions.take(5).forEach { expected ->
                val actual = db.transactionDao.getTransaction(expected.id)
                actual.id shouldBe expected.id
                actual.transactionType shouldBe expected.transactionType
            }

            documents.take(5).forEach { expected ->
                val actual = db.documentDao.getDocument(expected.id)
                actual.id shouldBe expected.id
                actual.displayName.isNotBlank().shouldBeTrue()
            }

            expenses.take(5).forEach { expected ->
                val actual = db.expenseDao.getExpense(expected.id).shouldNotBeNull()
                actual.id shouldBe expected.id
                actual.amount.shouldBeGreaterThan(0)
            }

            transactions
                .filter { it.transactionType == TransactionType.BUY }
                .take(3)
                .forEach { transaction ->
                    db.buyDao.getBuysWithDetails(transaction.id).forEach { buy ->
                        buy.productName.isNotBlank().shouldBeTrue()
                        buy.vendorName.isNotBlank().shouldBeTrue()
                        buy.count.shouldBeGreaterThan(0)
                    }
                }
        }
    }
})
