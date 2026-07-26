package ru.pavlig43.core.model

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class VendorNamesTest : FunSpec({
    test("vendor names are trimmed deduplicated and sorted") {
        listOf(" Стоинг ", "", "ингремарт", "СТОИНГ", "  ")
            .toVendorNamesText() shouldBe "ингремарт, Стоинг"
    }

    test("empty vendor list produces an empty cell") {
        listOf("", "  ").toVendorNamesText() shouldBe ""
    }
})
