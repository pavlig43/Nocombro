package ru.pavlig43.profitability.internal.di

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
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
            thenResult = "summary and product rows stay stable",
        )
    ) {
        withSeededTestDatabase { db ->
            val result = ProfitabilityRepository(db)
                .observeOnProducts(marchStart, marchEnd)
                .first()
                .getOrThrow()

            result.products.shouldHaveSize(1)

            result.summary.totalRevenue.value shouldBe 7_920_000L
            result.summary.batchExpenses.value shouldBe 1_253_170L
            result.summary.mainExpenses.value shouldBe 0L
            result.summary.profit.value shouldBe 6_666_830L
            result.summary.mainExpensesByType.shouldHaveSize(0)

            result.products.single().apply {
                productName shouldBe "Колбаски Баварские"
                quantity.value shouldBe 66_000L
                revenue.value shouldBe 7_920_000L
                totalExpenses.value shouldBe 1_253_170L
                expensesOnOneKg.value shouldBe 18_987L
                profit.value shouldBe 6_666_830L
                margin shouldBe (531.9972549614179 plusOrMinus 0.000001)
                profitability shouldBe (84.17714646464647 plusOrMinus 0.000001)
                details.shouldHaveSize(4)
            }
        }
    }
})
