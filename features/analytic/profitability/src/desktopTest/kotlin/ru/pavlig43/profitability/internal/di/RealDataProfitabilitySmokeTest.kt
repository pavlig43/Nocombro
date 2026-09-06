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
            thenResult = "product rows match their batch details",
        )
    ).config(enabled = realDataDatabasePath != null) {
        withCopiedTestDatabase(sourceDatabasePath = realDataDatabasePath.shouldNotBeNull()) { db ->
            val products = ProfitabilityRepository(db)
                .observeOnProducts(wideStart, wideEnd)
                .first()
                .getOrThrow()

            products.shouldNotBeEmpty()

            products.forEach { product ->
                product.revenue.value shouldBe product.details.sumOf { it.revenue.value }
                product.totalExpenses.value shouldBe product.details.sumOf { it.expenses.value }
                product.profit.value shouldBe product.details.sumOf { it.profit.value }
                product.quantity.value shouldBe product.details.sumOf { it.quantity.value }
                val expectedMargin = if (product.totalExpenses.value != 0L) {
                    product.profit.value.toDouble() / product.totalExpenses.value * 100
                } else {
                    0.0
                }
                product.margin.toBits() shouldBe expectedMargin.toBits()

                product.details.forEach { detail ->
                    detail.profit.value shouldBe detail.revenue.value - detail.expenses.value
                    val expectedDetailMargin = detail.profit.value.toDouble() / detail.expenses.value * 100
                    detail.margin.toBits() shouldBe expectedDetailMargin.toBits()
                }
            }
        }
    }
})
