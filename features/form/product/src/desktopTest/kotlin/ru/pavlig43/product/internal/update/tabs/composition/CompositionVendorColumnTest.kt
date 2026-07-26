package ru.pavlig43.product.internal.update.tabs.composition

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.product.ProductType
import ua.wwind.table.data.SortOrder
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.state.SortState

class CompositionVendorColumnTest : FunSpec({
    val alpha = CompositionUi(1, 1, 10, "Соль", "Стоинг", ProductType.FOOD_BASE, DecimalData3(500))
    val beta = CompositionUi(2, 2, 11, "Сахар", "Ингремарт", ProductType.FOOD_BASE, DecimalData3(500))

    test("composition sorts by supplier") {
        CompositionSorter.sort(
            listOf(alpha, beta),
            SortState(CompositionField.VENDOR_NAME, SortOrder.ASCENDING),
        ).map { it.composeId }.shouldContainExactly(2, 1)
    }

    test("composition filters by supplier") {
        CompositionFilterMatcher.matchesItem(
            alpha,
            mapOf(
                CompositionField.VENDOR_NAME to TableFilterState(
                    constraint = FilterConstraint.CONTAINS,
                    values = listOf("сто"),
                )
            ),
        ) shouldBe true
    }
})
