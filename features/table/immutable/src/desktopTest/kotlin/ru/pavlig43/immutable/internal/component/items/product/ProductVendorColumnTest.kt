package ru.pavlig43.immutable.internal.component.items.product

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.product.ProductType
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

class ProductVendorColumnTest : FunSpec({
    val alpha = ProductTableUi(
        composeId = 1,
        displayName = "А",
        vendorNames = "Стоинг",
        type = ProductType.FOOD_BASE,
        createdAt = LocalDate(2026, 1, 1),
    )
    val beta = ProductTableUi(
        composeId = 2,
        displayName = "Б",
        vendorNames = "Ингремарт",
        type = ProductType.FOOD_BASE,
        createdAt = LocalDate(2026, 1, 1),
    )

    test("products sort by supplier") {
        ProductSorter.sort(
            listOf(alpha, beta),
            SortState(ProductField.VENDOR_NAME, SortOrder.ASCENDING),
        ).map { it.composeId }.shouldContainExactly(2, 1)
    }

    test("products filter by supplier") {
        ProductFilterMatcher.matchesItem(
            alpha,
            mapOf(
                ProductField.VENDOR_NAME to TableFilterState(
                    constraint = FilterConstraint.CONTAINS,
                    values = listOf("сто"),
                )
            ),
        ) shouldBe true
    }
})
