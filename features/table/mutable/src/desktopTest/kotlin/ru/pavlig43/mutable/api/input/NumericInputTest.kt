package ru.pavlig43.mutable.api.input

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import ru.pavlig43.core.model.DecimalData2

class NumericInputTest : FunSpec({
    context("decimal input") {
        test("empty input is zero") {
            "".toDecimalInputOrNull(decimalPlaces = 2) shouldBe DecimalInput("", 0L)
        }

        test("a sole non-digit is rejected after becoming a dot") {
            ".".toDecimalInputOrNull(decimalPlaces = 2) shouldBe null
            ",".toDecimalInputOrNull(decimalPlaces = 2) shouldBe null
            "a".toDecimalInputOrNull(decimalPlaces = 2) shouldBe null
            "a".normalizeDecimalInput(decimalPlaces = 2) shouldBe "."
        }

        test("first non-digit between digits becomes a dot") {
            "12,34".toDecimalInputOrNull(decimalPlaces = 2) shouldBe DecimalInput("12.34", 1234L)
        }

        test("extra separators are ignored and decimal places are truncated") {
            "12,3.45".toDecimalInputOrNull(decimalPlaces = 2) shouldBe DecimalInput("12.34", 1234L)
        }

        test("overflow is rejected") {
            "92233720368547758.08".toDecimalInputOrNull(decimalPlaces = 2) shouldBe null
        }

        test("model text keeps scale without insignificant zeros") {
            DecimalData2(1150L).toDecimalInputText() shouldBe "11.5"
            DecimalData2(115000L).toDecimalInputText() shouldBe "1150"
            DecimalData2(115050L).toDecimalInputText() shouldBe "1150.5"
        }
    }

    context("integer input") {
        val range = 0..99

        test("empty input is zero") {
            "".toIntInputValueOrNull(range) shouldBe 0
        }

        test("range boundaries are accepted") {
            "0".toIntInputValueOrNull(range) shouldBe 0
            "99".toIntInputValueOrNull(range) shouldBe 99
        }

        test("letters are rejected") {
            "12a".toIntInputValueOrNull(range) shouldBe null
        }

        test("range overflow and integer overflow are rejected") {
            "100".toIntInputValueOrNull(range) shouldBe null
            "2147483648".toIntInputValueOrNull(0..Int.MAX_VALUE) shouldBe null
        }
    }
})
