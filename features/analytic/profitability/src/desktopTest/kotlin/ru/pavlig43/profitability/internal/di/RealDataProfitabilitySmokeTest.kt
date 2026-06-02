package ru.pavlig43.profitability.internal.di

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withCopiedTestDatabase
import ru.pavlig43.testkit.scenario
import kotlin.io.path.Path

private const val REAL_DATA_PROFITABILITY_DB_PATH_PROPERTY = "nocombro.realData.dbPath"

class RealDataProfitabilitySmokeTest : DesktopMainDispatcherFunSpec({

    val realDataDatabasePath = System.getProperty(REAL_DATA_PROFITABILITY_DB_PATH_PROPERTY)
        ?.takeIf { it.isNotBlank() }
        ?.let(::Path)

    val wideStart = LocalDateTime(2000, 1, 1, 0, 0)
    val wideEnd = LocalDateTime(2100, 1, 1, 0, 0)

    test(
        scenario(
            given = "a real copied database dump",
            whenAction = "profitability is calculated on a wide period",
            thenResult = "summary and product rows remain internally consistent",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath.shouldNotBeNull()) { db ->
            val result = ProfitabilityRepository(db)
                .observeOnProducts(wideStart, wideEnd)
                .first()
                .getOrThrow()

            result.products.shouldNotBeEmpty()

            result.summary.totalRevenue.value shouldBe result.products.sumOf { it.revenue.value }
            result.summary.batchExpenses.value shouldBe result.products.sumOf { it.totalExpenses.value }
            result.summary.mainExpenses.value shouldBe result.summary.mainExpensesByType.sumOf { it.amount.value }
            result.summary.profit.value shouldBe (
                result.summary.totalRevenue.value -
                    result.summary.batchExpenses.value -
                    result.summary.mainExpenses.value
                )

            result.products.take(10).forEach { product ->
                product.revenue.value shouldBe product.details.sumOf { it.revenue.value }
                product.totalExpenses.value shouldBe product.details.sumOf { it.expenses.value }
                product.profit.value shouldBe product.details.sumOf { it.profit.value }
                product.quantity.value shouldBe product.details.sumOf { it.quantity.value }
            }
        }
    }
})
