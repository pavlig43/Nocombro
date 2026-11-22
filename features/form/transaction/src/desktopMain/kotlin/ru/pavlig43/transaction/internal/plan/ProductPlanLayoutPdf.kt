package ru.pavlig43.transaction.internal.plan

import java.awt.Color
import java.awt.Desktop
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.math.floor
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.format
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDType0Font
import ru.pavlig43.datetime.dateFormat

private val layoutFileLock = Mutex()

internal suspend fun openProductPlanPdf(layout: ProductPlanLayout) = layoutFileLock.withLock {
    val folder = Path.of(System.getProperty("java.io.tmpdir"), "Nocombro")
    val target = replaceProductPlanPdf(layout, folder)
    try {
        Desktop.getDesktop().open(target.toFile())
    } catch (error: Exception) {
        throw IOException("Раскладка сохранена, но открыть PDF не удалось. Файл: $target", error)
    }
}

internal fun replaceProductPlanPdf(layout: ProductPlanLayout, folder: Path): Path {
    Files.createDirectories(folder)
    val target = folder.resolve("Раскладка.pdf")
    val pending = Files.createTempFile(folder, "production-layout-", ".tmp")
    try {
        writeProductPlanPdf(layout, pending)
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

internal fun writeProductPlanPdf(layout: ProductPlanLayout, target: Path) {
    PDDocument().use { document ->
        ProductPlanPdfWriter(document).write(layout)
        document.save(target.toFile())
    }
}

private const val MARGIN = 36f
private const val INGREDIENT_WIDTH = 247f
private const val CELL_PADDING = 11f
private const val TITLE_SIZE = 18f
private const val PLAN_SIZE = 11f
private const val TABLE_GAP = 21f
private const val HEADER_HEIGHT = 41f
private const val BATCH_LABEL_SIZE = 10f
private const val BATCH_MASS_SIZE = 12.5f
private const val BODY_SIZE = 11.5f
private const val BODY_LEADING = 14.5f
private const val ROW_PADDING = 7.25f
private const val MAX_BATCH_COLUMNS = 4

private val textColor = Color(22, 24, 26)
private val secondaryTextColor = Color(64, 69, 76)
private val gridColor = Color(55, 61, 70)
private val headerColor = Color(234, 236, 239)
private val stripeColor = Color(246, 247, 248)
private val footerLineColor = Color(155, 161, 170)

private class ProductPlanPdfWriter(private val document: PDDocument) {
    private val regular = font("Regular")
    private val bold = font("Bold")
    private val page = PDRectangle.A4
    private val width = page.width - 2 * MARGIN
    private val right = page.width - MARGIN
    private val contentTop = page.height - 75f
    private val contentBottom = 67f
    private val contentHeight = contentTop - contentBottom

    fun write(layout: ProductPlanLayout) {
        val products = layout.products.map(::measureProduct)
        val pages = paginate(products)
        val date = layout.createdAt.date.format(dateFormat)
        pages.forEachIndexed { index, tables ->
            val pdfPage = PDPage(page)
            document.addPage(pdfPage)
            PDPageContentStream(document, pdfPage).use { output ->
                val dateSize = 16f
                val dateLeft = (page.width - textWidth(date, dateSize, bold)) / 2
                output.text(date, dateLeft, page.height - 44f, dateSize, bold)
                tables.forEach { output.drawTable(it) }
                output.setStrokingColor(footerLineColor)
                output.setLineWidth(0.5f)
                output.line(MARGIN, 58f, right, 58f)
                output.stroke()
                output.rightText("Стр. ${index + 1} из ${pages.size}", right, 40f, 10.5f, regular, secondaryTextColor)
            }
        }
    }

    private fun measureProduct(product: ProductLayout): MeasuredProduct {
        val heading = measureHeading(product)
        val ingredientLines = product.recipe.map { wrap(it.name, INGREDIENT_WIDTH - 2 * CELL_PADDING, BODY_SIZE, regular) }
        val maxLinesPerRow = floor(
            (contentHeight - heading.height - HEADER_HEIGHT - 2 * ROW_PADDING) / BODY_LEADING,
        ).toInt()

        val weightsByPackages = mutableMapOf<Long, BatchWeights>()
        val tables = buildList {
            var firstBatch = 0L
            while (firstBatch < product.batches.batchCount) {
                val count = minOf(MAX_BATCH_COLUMNS.toLong(), product.batches.batchCount - firstBatch).toInt()
                val candidates = List(count) { offset ->
                    val index = firstBatch + offset
                    val packages = product.batches.packagesAt(index)
                    val weights = weightsByPackages.getOrPut(packages) { measureWeights(product, packages) }
                    val label = "Замес ${index + 1}"
                    val requiredWidth = maxOf(weights.requiredWidth, textWidth(label, BATCH_LABEL_SIZE, bold))
                    BatchColumn(label, weights, requiredWidth)
                }
                val columnCount = (count downTo 2).firstOrNull { columns ->
                    val available = batchColumnWidth(columns) - 2 * CELL_PADDING
                    candidates.take(columns).all { (_, _, requiredWidth) -> requiredWidth <= available }
                } ?: 1
                val columns = candidates.take(columnCount)
                val rows = ingredientLines.flatMapIndexed { index, lines ->
                    val weights = columns.map { it.weights.cells[index] }
                    lines.chunked(maxLinesPerRow).mapIndexed { part, fragment ->
                        IngredientRow(
                            lines = fragment,
                            weights = if (part == 0) weights else emptyList(),
                            shaded = index % 2 != 0,
                            continues = lines.size > maxLinesPerRow,
                        )
                    }
                }
                add(MeasuredTable(heading, columns, rows))
                firstBatch += columnCount
            }
        }
        return MeasuredProduct(tables)
    }

    private fun measureHeading(product: ProductLayout): ProductHeading {
        val plan = "План ${kgText(product.plannedGrams)} кг"
        val available = width - textWidth(plan, PLAN_SIZE, bold) - CELL_PADDING
        val maxHeight = contentHeight - HEADER_HEIGHT - BODY_LEADING - 2 * ROW_PADDING
        var size = TITLE_SIZE
        while (true) {
            val heading = ProductHeading(wrap(product.name, available, size, bold), plan, size)
            if (heading.height <= maxHeight) return heading
            size /= 2
        }
    }

    private fun measureWeights(product: ProductLayout, packages: Long): BatchWeights {
        val grams = packages * product.packageGrams
        val mass = "${kgText(grams)} кг"
        val cells = weighIngredients(product.recipe, grams).map(::kgText)
        val widestCell = cells.maxOfOrNull { textWidth(it, BODY_SIZE, regular) } ?: 0f
        return BatchWeights(mass, cells, maxOf(textWidth(mass, BATCH_MASS_SIZE, bold), widestCell))
    }

    private fun paginate(products: List<MeasuredProduct>): List<List<PlacedTable>> {
        val pages = mutableListOf(mutableListOf<PlacedTable>())
        var top = contentTop

        fun ensureSpace(height: Float) {
            if (height > top - contentBottom && pages.last().isNotEmpty()) {
                pages.add(mutableListOf())
                top = contentTop
            }
        }

        for (product in products) {
            if (product.height <= contentHeight) ensureSpace(product.height)
            for (table in product.tables) {
                if (table.height <= contentHeight) {
                    ensureSpace(table.height)
                    pages.last().add(PlacedTable(table, table.rows, top))
                    top -= table.height + TABLE_GAP
                    continue
                }

                var start = 0
                while (start < table.rows.size) {
                    ensureSpace(table.headingAndHeaderHeight + table.rows[start].height)
                    var available = top - contentBottom - table.headingAndHeaderHeight
                    var end = start
                    while (end < table.rows.size && table.rows[end].height <= available) {
                        available -= table.rows[end].height
                        end++
                    }
                    pages.last().add(PlacedTable(table, table.rows.subList(start, end), top))
                    top = contentBottom + available - TABLE_GAP
                    start = end
                }
            }
        }
        return pages
    }

    private fun PDPageContentStream.drawTable(placed: PlacedTable) {
        val (table, rows, top) = placed
        val heading = table.heading
        val baseline = top - maxOf(heading.size, PLAN_SIZE)
        heading.lines.forEachIndexed { index, name ->
            text(name, MARGIN, baseline - index * heading.leading, heading.size, bold)
        }
        rightText(heading.plan, right, baseline + 2f, PLAN_SIZE, bold, secondaryTextColor)

        val gridTop = top - heading.height
        val headerBottom = gridTop - HEADER_HEIGHT
        val columnWidth = batchColumnWidth(table.columns.size)
        fun batchTextRight(index: Int) = MARGIN + INGREDIENT_WIDTH + (index + 1) * columnWidth - CELL_PADDING

        fill(MARGIN, gridTop, width, HEADER_HEIGHT, headerColor)
        text("Ингредиент", MARGIN + CELL_PADDING, gridTop - 25f, BODY_SIZE, bold)
        table.columns.forEachIndexed { index, column ->
            rightText(column.label, batchTextRight(index), gridTop - 15f, BATCH_LABEL_SIZE, bold, secondaryTextColor)
            rightText(column.weights.mass, batchTextRight(index), gridTop - 32f, BATCH_MASS_SIZE, bold)
        }

        var rowTop = headerBottom
        rows.forEach { row ->
            if (row.shaded) fill(MARGIN, rowTop, width, row.height, stripeColor)
            val firstBaseline = rowTop - ROW_PADDING - (BODY_LEADING + BODY_SIZE) / 2
            row.lines.forEachIndexed { index, line ->
                text(line, MARGIN + CELL_PADDING, firstBaseline - index * BODY_LEADING, BODY_SIZE, regular)
            }
            val weightBaseline = firstBaseline - if (row.continues) 0f else (row.lines.size - 1) * BODY_LEADING / 2
            row.weights.forEachIndexed { index, weight ->
                rightText(weight, batchTextRight(index), weightBaseline, BODY_SIZE, regular)
            }
            rowTop -= row.height
        }

        setStrokingColor(gridColor)
        setLineWidth(1f)
        var boundary = headerBottom
        rows.dropLast(1).forEach { row ->
            boundary -= row.height
            line(MARGIN, boundary, right, boundary)
        }
        table.columns.indices.forEach { index ->
            val x = MARGIN + INGREDIENT_WIDTH + index * columnWidth
            line(x, gridTop, x, rowTop)
        }
        stroke()
        setLineWidth(1.4f)
        addRect(MARGIN, rowTop, width, gridTop - rowTop)
        if (rows.isNotEmpty()) line(MARGIN, headerBottom, right, headerBottom)
        stroke()
    }

    private fun batchColumnWidth(count: Int) = (width - INGREDIENT_WIDTH) / count

    private fun PDPageContentStream.fill(x: Float, top: Float, width: Float, height: Float, color: Color) {
        setNonStrokingColor(color)
        addRect(x, top - height, width, height)
        fill()
    }

    private fun PDPageContentStream.line(x1: Float, y1: Float, x2: Float, y2: Float) {
        moveTo(x1, y1)
        lineTo(x2, y2)
    }

    private fun PDPageContentStream.rightText(
        value: String, right: Float, y: Float, size: Float, font: PDType0Font, color: Color = textColor,
    ) = text(value, right - textWidth(value, size, font), y, size, font, color)

    private fun PDPageContentStream.text(
        value: String, x: Float, y: Float, size: Float, font: PDType0Font, color: Color = textColor,
    ) {
        setNonStrokingColor(color)
        beginText()
        setFont(font, size)
        newLineAtOffset(x, y)
        showText(value)
        endText()
    }

    private fun font(weight: String): PDType0Font {
        val path = "composeResources/ru.pavlig43.theme/font/MontserratAlternates-$weight.ttf"
        return checkNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Шрифт раскладки недоступен" }
            .use { PDType0Font.load(document, it, true) }
    }

    private fun printable(value: String, font: PDType0Font): String =
        value.codePoints().toArray().joinToString("") { codePoint ->
            val character = String(Character.toChars(codePoint))
            when {
                Character.isWhitespace(codePoint) || Character.isISOControl(codePoint) -> " "
                runCatching { font.encode(character) }.isSuccess -> character
                else -> "?"
            }
        }

    private fun wrap(value: String, available: Float, size: Float, font: PDType0Font): List<String> {
        val words = printable(value, font).split(' ').filter { it.isNotEmpty() }
        val result = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (textWidth(candidate, size, font) <= available) {
                current = candidate
                continue
            }
            if (current.isNotEmpty()) result += current
            current = ""
            word.codePoints().toArray().forEach {
                val character = String(Character.toChars(it))
                if (current.isNotEmpty() && textWidth(current + character, size, font) > available) {
                    result += current
                    current = ""
                }
                current += character
            }
        }
        if (current.isNotEmpty()) result += current
        return result.ifEmpty { listOf("") }
    }

    private fun textWidth(value: String, size: Float, font: PDType0Font) =
        font.getStringWidth(value) * size / 1000
}

private data class ProductHeading(val lines: List<String>, val plan: String, val size: Float) {
    val leading = size + 4f
    val height = maxOf(size, PLAN_SIZE) + (lines.size - 1) * leading + 15f
}

private data class BatchWeights(val mass: String, val cells: List<String>, val requiredWidth: Float)

private data class BatchColumn(val label: String, val weights: BatchWeights, val requiredWidth: Float)

private data class IngredientRow(
    val lines: List<String>,
    val weights: List<String>,
    val shaded: Boolean,
    val continues: Boolean,
) {
    val height = lines.size * BODY_LEADING + 2 * ROW_PADDING
}

private data class MeasuredTable(val heading: ProductHeading, val columns: List<BatchColumn>, val rows: List<IngredientRow>) {
    val headingAndHeaderHeight = heading.height + HEADER_HEIGHT
    val height = headingAndHeaderHeight + rows.sumOf { it.height.toDouble() }.toFloat()
}

private data class MeasuredProduct(val tables: List<MeasuredTable>) {
    val height = tables.sumOf { it.height.toDouble() }.toFloat() + (tables.size - 1).coerceAtLeast(0) * TABLE_GAP
}

private data class PlacedTable(val table: MeasuredTable, val rows: List<IngredientRow>, val top: Float)
