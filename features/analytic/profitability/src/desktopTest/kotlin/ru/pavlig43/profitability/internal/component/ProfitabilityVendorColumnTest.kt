package ru.pavlig43.profitability.internal.component

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.profitability.internal.di.ProfitabilityRepository
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec
import ru.pavlig43.testkit.database.withSeededTestDatabase
import ru.pavlig43.testkit.scenario
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

class ProfitabilityVendorColumnTest : DesktopMainDispatcherFunSpec({
    test(
        scenario(
            given = "seeded sales",
            whenAction = "profitability rows are calculated",
            thenResult = "supplier comes from sold batches and totals stay unchanged",
        )
    ) {
        withSeededTestDatabase { db ->
            val result = ProfitabilityRepository(db).observeOnProducts(
                LocalDateTime(2026, 3, 1, 0, 0),
                LocalDateTime(2026, 3, 31, 23, 59, 59),
            ).first().getOrThrow()
            val product = result.products.single()

            product.vendorNames shouldBe "ИП Гармаш"
            result.summary.totalRevenue.value shouldBe 7_920_000L
            ProfitabilityFilterMatcher.matchesItem(
                product,
                mapOf(
                    ProfitabilityField.VENDOR_NAME to TableFilterState(
                        constraint = FilterConstraint.CONTAINS,
                        values = listOf("гармаш"),
                    )
                ),
            ) shouldBe true
            ProfitabilitySorter.sort(
                listOf(product),
                SortState(ProfitabilityField.VENDOR_NAME, SortOrder.ASCENDING),
            ).single() shouldBe product
        }
    }
})
