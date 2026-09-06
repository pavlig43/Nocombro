package ru.pavlig43.profitability.internal.di

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.batch.StorageLocation
import ru.pavlig43.database.data.expense.ExpenseBD
import ru.pavlig43.database.data.expense.ExpenseType
import ru.pavlig43.database.data.storage.StorageOperationsRepository
import ru.pavlig43.database.data.storage.StorageWriteOffRequest
import ru.pavlig43.database.data.transact.StockOperationReason
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.scenario

class ProfitabilitySmokeTest : DesktopMainDispatcherFunSpec({

    val marchStart = LocalDateTime(2026, 3, 1, 0, 0)
    val marchEnd = LocalDateTime(2026, 3, 31, 23, 59, 59)

    test(
        scenario(
            given = "the seeded database",
            whenAction = "profitability is calculated for March 2026",
            thenResult = "product and batch totals stay stable",
        )
    ) {
        withSeededTestDatabase { db ->
            val products = ProfitabilityRepository(db)
                .observeOnProducts(marchStart, marchEnd)
                .first()
                .getOrThrow()

            products.shouldHaveSize(1)

            products.single().apply {
                productName shouldBe "Колбаски Баварские"
                quantity.value shouldBe 66_000L
                revenue.value shouldBe 7_920_000L
                totalExpenses.value shouldBe 1_253_170L
                expensesOnOneKg.value shouldBe 18_987L
                profit.value shouldBe 6_666_830L
                margin shouldBe (531.9972549614179 plusOrMinus 0.000001)
                details.shouldHaveSize(4)
                quantity.value shouldBe details.sumOf { it.quantity.value }
                revenue.value shouldBe details.sumOf { it.revenue.value }
                totalExpenses.value shouldBe details.sumOf { it.expenses.value }
                profit.value shouldBe details.sumOf { it.profit.value }
                details.forEach { detail ->
                    detail.margin shouldBe (
                        (detail.profit.value.toDouble() / detail.expenses.value * 100) plusOrMinus 0.000001
                    )
                }
            }
        }
    }

    test("general expenses and material write offs do not change product rows") {
        withSeededTestDatabase { db ->
            val repository = ProfitabilityRepository(db)
            val before = repository.observeOnProducts(marchStart, marchEnd).first().getOrThrow()

            db.expenseDao.insertExpense(
                ExpenseBD(
                    transactionId = null,
                    expenseType = ExpenseType.OTHER,
                    amount = 999_999L,
                    expenseDateTime = LocalDateTime(2026, 3, 15, 12, 0),
                    comment = "profitability test",
                )
            )
            val afterGeneralExpense = repository
                .observeOnProducts(marchStart, marchEnd)
                .first()
                .getOrThrow()

            val operations = StorageOperationsRepository(db)
            operations.writeOff(
                StorageWriteOffRequest(
                    batchId = 1,
                    source = StorageLocation.MAIN,
                    count = 1_000,
                    occurredAt = LocalDateTime(2026, 3, 20, 12, 0),
                    reason = StockOperationReason.EXPERIMENT,
                    comment = "profitability test",
                )
            ).getOrThrow()
            val afterMaterialWriteOff = repository
                .observeOnProducts(marchStart, marchEnd)
                .first()
                .getOrThrow()

            afterGeneralExpense shouldBe before
            afterMaterialWriteOff shouldBe before
        }
    }
})
