package ru.pavlig43.immutable.internal.component.items.product

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.immutable.internal.column.toTableDisplayText
import ru.pavlig43.immutable.internal.component.filterBySearch

class ProductSearchMatcherTest : FunSpec({
    val productType = ProductType.entries.first()
    val date = LocalDate(2026, 7, 31)
    val product = ProductTableUi(
        composeId = 741,
        displayName = "Стальной вал",
        vendorNames = "North Wind",
        type = productType,
        createdAt = date,
        secondName = "SN-АБВ-42",
        comment = "Для тестовой партии",
    )

    test("finds every displayed product field without case sensitivity") {
        listOf(
            "741",
            "стальной",
            "NORTH WIND",
            productType.displayName.lowercase(),
            date.toTableDisplayText(),
            "абв-42",
            "ТЕСТОВОЙ ПАРТИИ",
        ).forEach { query ->
            listOf(product).filterBySearch(query, ProductSearchMatcher) shouldBe listOf(product)
        }
    }

    test("trims the query and returns all rows for an empty query") {
        listOf(product).filterBySearch("  стальной  ", ProductSearchMatcher) shouldBe listOf(product)
        listOf(product).filterBySearch("   ", ProductSearchMatcher) shouldBe listOf(product)
    }

    test("returns no rows for missing text") {
        listOf(product).filterBySearch("керамическая втулка", ProductSearchMatcher) shouldBe emptyList()
    }
})
