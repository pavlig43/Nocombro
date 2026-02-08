package ru.pavlig43.core

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Пример теста для проверки работы TestingPlugin и Kotest.
 */
class SampleTest : FunSpec({
    test("addition should work") {
        // Arrange
        val a = 2
        val b = 3

        // Act
        val result = a + b

        // Assert
        result shouldBe 5
    }

    test("string concatenation should work") {
        val hello = "Hello"
        val world = "World"

        val result = "$hello, $world!"

        result shouldBe "Hello, World!"
    }
})
