package ru.pavlig43.transaction.internal.production

import java.io.IOException
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font

private val layoutFileLock = Mutex()

internal suspend fun openProductionPdf(layout: ProductionLayout) = layoutFileLock.withLock {
    val folder = Path.of(System.getProperty("java.io.tmpdir"), "Nocombro")
    val target = replaceProductionPdf(layout, folder)
    try {
        Desktop.getDesktop().open(target.toFile())
    } catch (error: Exception) {
        throw IOException("Раскладка сохранена, но открыть PDF не удалось. Файл: $target", error)
    }
}

internal fun replaceProductionPdf(layout: ProductionLayout, folder: Path): Path {
    Files.createDirectories(folder)
    val target = folder.resolve("Раскладка.pdf")
    val pending = Files.createTempFile(folder, "production-layout-", ".tmp")
    try {
        writeProductionPdf(layout, pending)
        try {
            Files.move(pending, target, ATOMIC_MOVE, REPLACE_EXISTING)
        } catch (error: IOException) {
            throw IOException("Не удалось заменить раскладку. Если PDF открыт, закройте его и повторите.", error)
        }
        return target
    } finally {
        Files.deleteIfExists(pending)
    }
}

internal fun writeProductionPdf(layout: ProductionLayout, target: Path) {
    PDDocument().use { document ->
        ProductionPdfWriter(document).use { it.write(layout) }
        document.save(target.toFile())
    }
}

private class ProductionPdfWriter(private val document: PDDocument) : AutoCloseable {
    private val regular = font("Regular")
    private val bold = font("Bold")
    private val page = PDRectangle.A4
    private val margin = 32f
    private val gap = 16f
    private val width = (page.width - margin * 2 - gap) / 2
    private val line = 14f
    private var stream: PDPageContentStream? = null
    private var y = 0f

    private fun font(weight: String): PDType0Font {
        val path = "composeResources/ru.pavlig43.theme/font/MontserratAlternates-$weight.ttf"
        return checkNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Шрифт раскладки недоступен" }
            .use { PDType0Font.load(document, it, true) }
    }

    fun write(layout: ProductionLayout) {
        newPage()
        heading(layout.title, 16f)
        layout.products.forEach { product ->
            if (y < margin + 110f) newPage()
            productHeading(product)
            val entries = product.recipe.flatMapIndexed { index, ingredient ->
                wrap(ingredient.name, width - 78f, 9f, regular).mapIndexed { part, label ->
                    PdfIngredientLine(label, index.takeIf { part == 0 })
                }
            }
            val weightsByPackageCount = mutableMapOf<Long, List<Long>>()
            var firstBatch = 0L
            while (firstBatch < product.batches.batchCount) {
                val secondBatch = firstBatch + 1
                val pairSize = if (secondBatch < product.batches.batchCount) 2 else 1
                val weights = List(pairSize) { offset ->
                    val packages = product.batches.packagesAt(firstBatch + offset)
                    weightsByPackageCount.getOrPut(packages) {
                        weighIngredients(product.recipe, packages * product.packageGrams)
                    }
                }
                var start = 0
                while (start < entries.size) {
                    val maxLines = ((y - margin - 28f) / line).toInt()
                    val end = minOf(entries.size, start + maxLines.coerceAtLeast(0))
                    if (end == start || (start == 0 && end < entries.size && y < page.height / 2)) {
                        newPage()
                        productHeading(product, continuation = true)
                        continue
                    }
                    repeat(pairSize) { offset ->
                        val index = firstBatch + offset
                        val packages = product.batches.packagesAt(index)
                        val label = "Замес №${index + 1}   ${kg(packages * product.packageGrams)}   ($packages пак.)"
                        block(
                            margin + offset * (width + gap),
                            if (start == 0) label else "Замес №${index + 1} (продолжение)",
                            entries.subList(start, end), weights[offset],
                        )
                    }
                    y -= (end - start + 1) * line + 14f
                    start = end
                }
                if (pairSize == 1) break
                firstBatch += 2
            }
            y -= 10f
        }
    }

    private fun productHeading(product: ProductLayout, continuation: Boolean = false) {
        val name = if (continuation) product.name.take(120) else product.name
        heading(name, 13f)
        text("План ${kg(product.plannedGrams)}   •   Мешалка ${kg(product.capacity)}   •   Пакет ${kg(product.packageGrams)}",
            margin, y, 10f, regular)
        y -= 23f
    }

    private fun heading(label: String, size: Float) {
        val lines = wrap(label, page.width - margin * 2, size, bold)
        lines.forEach {
            if (y < margin + size) newPage()
            text(it, margin, y, size, bold)
            y -= size + 5f
        }
    }

    private fun block(x: Float, title: String, entries: List<PdfIngredientLine>, weights: List<Long>) {
        val output = checkNotNull(stream)
        val top = y + 4
        val height = (entries.size + 1) * line + 4
        output.setLineWidth(0.5f)
        output.addRect(x, top - height, width, height)
        output.moveTo(x, top - line - 3)
        output.lineTo(x + width, top - line - 3)
        output.stroke()
        val titleSize = minOf(9f, 9f * (width - 10f) / textWidth(title, 9f, bold))
        text(title, x + 5, y - 7f, titleSize, bold)
        entries.forEachIndexed { i, entry ->
            val baseline = y - (i + 1) * line - 7f
            text(entry.label, x + 5, baseline, 9f, regular)
            entry.ingredientIndex?.let {
                val weight = kg(weights[it])
                text(weight, x + width - 5 - textWidth(weight, 9f, regular), baseline, 9f, regular)
            }
        }
    }

    private fun text(value: String, x: Float, y: Float, size: Float, font: PDType0Font) {
        val output = checkNotNull(stream)
        output.beginText()
        output.setFont(font, size)
        output.newLineAtOffset(x, y)
        output.showText(printable(value, font))
        output.endText()
    }

    private fun printable(value: String, font: PDType0Font): String =
        value.codePoints().toArray().joinToString("") { codePoint ->
            val character = String(Character.toChars(codePoint))
            when {
                Character.isISOControl(codePoint) -> " "
                runCatching { font.encode(character) }.isSuccess -> character
                else -> "?"
            }
        }

    private fun wrap(value: String, available: Float, size: Float, font: PDType0Font): List<String> {
        val words = printable(value, font).split(Regex("\\s+")).filter { it.isNotBlank() }
        val result = mutableListOf<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textWidth(candidate, size, font) <= available) {
                current = candidate
            } else {
                if (current.isNotEmpty()) result += current
                current = ""
                word.codePoints().toArray().forEach {
                    val character = String(Character.toChars(it))
                    if (textWidth(current + character, size, font) > available) {
                        result += current
                        current = ""
                    }
                    current += character
                }
            }
        }
        if (current.isNotEmpty()) result += current
        return result.ifEmpty { listOf("") }
    }

    private fun textWidth(value: String, size: Float, font: PDType0Font) =
        font.getStringWidth(value) * size / 1000

    private fun newPage() {
        close()
        val nextPage = PDPage(page)
        document.addPage(nextPage)
        stream = PDPageContentStream(document, nextPage)
        y = page.height - margin
    }

    override fun close() {
        stream?.close()
        stream = null
    }

    private fun kg(grams: Long) = "${kgText(grams)} кг"
    private data class PdfIngredientLine(val label: String, val ingredientIndex: Int?)
}
