package ru.pavlig43.product.internal.update.tabs.specification

import java.io.File
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import ru.pavlig43.database.data.product.ProductSpecification

private const val THEME_FONT_RESOURCE_ROOT = "composeResources/ru.pavlig43.theme/font"

/**
 * Генерирует печатную PDF-версию спецификации продукта.
 *
 * Генератор работает вне Compose и напрямую через PDFBox, поэтому для кириллицы
 * ему нужен доступ к TTF-шрифту, который можно встроить в PDF. Шрифт берется
 * из ресурсов проекта, а не из системной папки Windows.
 */
internal class ProductSpecificationPdfGenerator {

    /**
     * Создаёт PDF-файл спецификации по указанному локальному пути.
     */
    fun generate(
        outputPath: String,
        productName: String,
        specification: ProductSpecification,
    ) {
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()

        PDDocument().use { document ->
            val regularFont = loadFont(
                document = document,
                resourcePaths = listOf(
                    "$THEME_FONT_RESOURCE_ROOT/MontserratAlternates-Regular.ttf",
                ),
            ) ?: error("Не удалось найти regular TTF-шрифт для генерации PDF спецификации.")
            val boldFont = loadFont(
                document = document,
                resourcePaths = listOf(
                    "$THEME_FONT_RESOURCE_ROOT/MontserratAlternates-Bold.ttf",
                ),
            ) ?: regularFont

            ProductSpecificationPdfWriter(
                document = document,
                regularFont = regularFont,
                boldFont = boldFont,
            ).write(
                productName = productName,
                specification = specification,
            )

            document.save(outputFile)
        }
    }

    /**
     * Возвращает первый доступный TTF-шрифт из списка resource-path кандидатов.
     */
    @Suppress("ReturnCount")
    private fun loadFont(
        document: PDDocument,
        resourcePaths: List<String>,
    ): PDType0Font? {
        val classLoader = javaClass.classLoader
        val resourcePath = resourcePaths.firstOrNull { path ->
            classLoader.getResource(path) != null
        } ?: return null

        classLoader.getResourceAsStream(resourcePath)?.use { inputStream ->
            return PDType0Font.load(document, inputStream, true)
        }
        return null
    }
}

/**
 * Низкоуровневый writer поверх PDFBox, который занимается пагинацией и
 * переносом строк.
 */
@Suppress("TooManyFunctions")
private class ProductSpecificationPdfWriter(
    private val document: PDDocument,
    private val regularFont: PDFont,
    private val boldFont: PDFont,
) {
    private val pageWidth = PDRectangle.A4.width
    private val pageHeight = PDRectangle.A4.height
    private val margin = 48f
    private val contentWidth = pageWidth - margin * 2

    private lateinit var page: PDPage
    private lateinit var stream: PDPageContentStream
    private var y = 0f

    /**
     * Рисует содержимое всей спецификации в документ.
     */
    @Suppress("MagicNumber")
    fun write(
        productName: String,
        specification: ProductSpecification,
    ) {
        newPage()

        drawParagraph("Спецификация", boldFont, 18f, 22f)
        drawRule()
        spacer(10f)

        drawParagraph("Наименование: $productName", boldFont, 12f, 16f)
        spacer(6f)

        drawSection("Дозировка", specification.dosage)
        drawSection("Состав", specification.composition)
        drawSection(
            "Хранение и срок годности",
            buildList {
                specification.shelfLifeText.takeIf(String::isNotBlank)?.let {
                    add("Срок годности: $it")
                }
                specification.storageConditions.takeIf(String::isNotBlank)?.let {
                    add("Условия хранения: $it")
                }
            }.joinToString("\n"),
        )
        drawSection(
            "Органолептические показатели",
            buildList {
                specification.appearance.takeIf(String::isNotBlank)?.let {
                    add("Внешний вид: $it")
                }
                specification.color.takeIf(String::isNotBlank)?.let {
                    add("Цвет: $it")
                }
                specification.smell.takeIf(String::isNotBlank)?.let {
                    add("Запах: $it")
                }
                specification.taste.takeIf(String::isNotBlank)?.let {
                    add("Вкус: $it")
                }
            }.joinToString("\n"),
        )
        drawSection("Физико-химические показатели", specification.physicalChemicalIndicators)
        drawSection("Микробиологические показатели", specification.microbiologicalIndicators)
        drawSection("Содержание токсичных элементов", specification.toxicElements)
        drawAllergensSection(specification.allergens)
        drawSection("Информация о ГМО", specification.gmoInfo)

        stream.close()
    }

    /**
     * Добавляет секцию только если в ней есть содержимое.
     *
     * Пустые поля намеренно не печатаются, чтобы в PDF не было строк вида
     * `Цвет: -`.
     */
    @Suppress("MagicNumber")
    private fun drawSection(
        title: String,
        content: String,
    ) {
        if (content.isBlank()) return
        drawParagraph(title, boldFont, 12f, 16f)
        drawParagraph(content, regularFont, 10.5f, 14f)
        spacer(8f)
    }

    /**
     * Рисует блок аллергенов таблицей с явной шапкой колонок.
     */
    @Suppress("MagicNumber")
    private fun drawAllergensSection(
        content: String,
    ) {
        if (content.isBlank()) return

        val bodyRows = decodeAllergensJson(content)
            .map { row ->
                listOf(row.name, row.inProduct, row.inProduction)
            }
        val rows = listOf(
            listOf("Аллерген", "В продукте", "На производстве")
        ) + bodyRows

        if (bodyRows.isEmpty() || rows.any { it.size < 3 }) {
            drawSection("Аллергены", content)
            return
        }

        val columnRatios = listOf(0.56f, 0.22f, 0.22f)
        val totalTableHeight = measureTableHeight(
            rows = rows,
            columnRatios = columnRatios,
        )
        ensureSpace(16f + totalTableHeight + 8f)
        drawParagraph("Аллергены", boldFont, 12f, 16f)
        drawTable(
            rows = rows,
            columnRatios = columnRatios,
        )
        spacer(10f)
    }

    /**
     * Рисует абзац с автоматическим переносом и учётом границ страницы.
     */
    private fun drawParagraph(
        text: String,
        font: PDFont,
        fontSize: Float,
        lineHeight: Float,
    ) {
        val paragraphs = text
            .replace("\r\n", "\n")
            .split('\n')
            .ifEmpty { listOf("") }

        paragraphs.forEachIndexed { index, paragraph ->
            val lines = wrapLine(paragraph.ifBlank { " " }, font, fontSize)
            lines.forEach { line ->
                ensureSpace(lineHeight)
                stream.beginText()
                stream.setFont(font, fontSize)
                stream.newLineAtOffset(margin, y)
                stream.showText(line)
                stream.endText()
                y -= lineHeight
            }

            if (index != paragraphs.lastIndex) {
                spacer(lineHeight / 2f)
            }
        }
    }

    /**
     * Делит длинную строку на строки, которые помещаются по ширине.
     */
    private fun wrapLine(
        text: String,
        font: PDFont,
        fontSize: Float,
    ): List<String> {
        if (text.isBlank()) return listOf(" ")

        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
            val candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize
            if (candidateWidth <= contentWidth || currentLine.isBlank()) {
                currentLine = candidate
            } else {
                lines += currentLine
                currentLine = word
            }
        }

        if (currentLine.isNotBlank()) {
            lines += currentLine
        }

        return lines.ifEmpty { listOf(" ") }
    }

    @Suppress("MagicNumber")
    private fun drawRule() {
        ensureSpace(12f)
        stream.moveTo(margin, y)
        stream.lineTo(pageWidth - margin, y)
        stream.stroke()
        y -= 12f
    }

    private fun spacer(height: Float) {
        ensureSpace(height)
        y -= height
    }

    /**
     * Рисует простую таблицу с рамками и переносами текста внутри ячеек.
     */
    private fun drawTable(
        rows: List<List<String>>,
        columnRatios: List<Float>,
    ) {
        ensureSpace(measureTableHeight(rows, columnRatios))
        val columnWidths = columnRatios.map { ratio -> contentWidth * ratio }
        val horizontalPadding = 6f
        val verticalPadding = 5f
        val fontSize = 9.5f
        val lineHeight = 12f

        rows.forEachIndexed { rowIndex, row ->
            val preparedCells = row.take(columnWidths.size).mapIndexed { index, cellText ->
                wrapTextToWidth(
                    text = cellText,
                    font = if (rowIndex == 0) boldFont else regularFont,
                    fontSize = fontSize,
                    maxWidth = columnWidths[index] - horizontalPadding * 2,
                )
            }
            val rowHeight = preparedCells.maxOfOrNull { cellLines ->
                cellLines.size * lineHeight + verticalPadding * 2
            } ?: (lineHeight + verticalPadding * 2)

            ensureSpace(rowHeight + 2f)
            val top = y
            val bottom = y - rowHeight
            var currentX = margin

            preparedCells.forEachIndexed { cellIndex, cellLines ->
                val cellWidth = columnWidths[cellIndex]
                stream.addRect(currentX, bottom, cellWidth, rowHeight)
                stream.stroke()

                var textY = top - verticalPadding - fontSize
                cellLines.forEach { line ->
                    stream.beginText()
                    stream.setFont(if (rowIndex == 0) boldFont else regularFont, fontSize)
                    stream.newLineAtOffset(currentX + horizontalPadding, textY)
                    stream.showText(line)
                    stream.endText()
                    textY -= lineHeight
                }

                currentX += cellWidth
            }

            y = bottom
        }
    }

    /**
     * Считает итоговую высоту таблицы, чтобы можно было перенести её целиком
     * на следующую страницу и не рвать между страницами.
     */
    private fun measureTableHeight(
        rows: List<List<String>>,
        columnRatios: List<Float>,
    ): Float {
        val columnWidths = columnRatios.map { ratio -> contentWidth * ratio }
        val horizontalPadding = 6f
        val verticalPadding = 5f
        val fontSize = 9.5f
        val lineHeight = 12f

        return rows.sumOf { row ->
            val rowHeight = row.take(columnWidths.size).mapIndexed { index, cellText ->
                val lines = wrapTextToWidth(
                    text = cellText,
                    font = regularFont,
                    fontSize = fontSize,
                    maxWidth = columnWidths[index] - horizontalPadding * 2,
                )
                lines.size * lineHeight + verticalPadding * 2
            }.maxOrNull() ?: (lineHeight + verticalPadding * 2)
            rowHeight.toDouble()
        }.toFloat()
    }

    /**
     * Делит текст на строки под конкретную ширину ячейки таблицы.
     */
    private fun wrapTextToWidth(
        text: String,
        font: PDFont,
        fontSize: Float,
        maxWidth: Float,
    ): List<String> {
        if (text.isBlank()) return listOf("")

        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val candidate = if (currentLine.isBlank()) word else "$currentLine $word"
            val candidateWidth = font.getStringWidth(candidate) / 1000f * fontSize
            if (candidateWidth <= maxWidth || currentLine.isBlank()) {
                currentLine = candidate
            } else {
                lines += currentLine
                currentLine = word
            }
        }

        if (currentLine.isNotBlank()) {
            lines += currentLine
        }

        return lines.ifEmpty { listOf("") }
    }

    /**
     * Переходит на новую страницу, если на текущей уже не хватает места.
     */
    private fun ensureSpace(requiredHeight: Float) {
        if (!::stream.isInitialized) {
            newPage()
            return
        }
        if (y - requiredHeight < margin) {
            stream.close()
            newPage()
        }
    }

    /**
     * Открывает новую страницу и новый content stream.
     */
    private fun newPage() {
        page = PDPage(PDRectangle.A4)
        document.addPage(page)
        stream = PDPageContentStream(document, page)
        y = pageHeight - margin
    }
}
