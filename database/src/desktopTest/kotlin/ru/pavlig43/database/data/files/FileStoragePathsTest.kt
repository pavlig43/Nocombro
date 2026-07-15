package ru.pavlig43.database.data.files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/** Проверяет нормализацию и защитные границы логических ключей файлов. */
class FileStoragePathsTest : FunSpec({
    test("logical file key normalizes ordinary backslashes") {
        normalizeLogicalFileKey("files\\product\\report.pdf") shouldBe
            "files/product/report.pdf"
    }

    test("logical file key rejects unsafe paths") {
        listOf(
            "",
            "   ",
            "/absolute",
            "\\absolute",
            "C:/absolute",
            "C:\\absolute",
            "C:relative.txt",
            "files/./report.pdf",
            "files/../report.pdf",
            "files//report.pdf",
        ).forEach { key ->
            shouldThrow<IllegalArgumentException> { normalizeLogicalFileKey(key) }
        }
    }
})
