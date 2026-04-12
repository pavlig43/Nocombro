package ru.pavlig43.product.internal.update.tabs.specification

import java.io.File
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.apache.pdfbox.pdmodel.font.PDFont
import org.apache.pdfbox.pdmodel.font.PDType0Font
import ru.pavlig43.database.data.product.ProductSpecification

internal class ProductSpecificationPdfGenerator {

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
                candidates = listOf(
                    File("C:/Windows/Fonts/arial.ttf"),
                    File("C:/Windows/Fonts/ARIALUNI.TTF"),
                    File("theme/src/commonMain/composeResources/font/MontserratAlternates-Regular.ttf"),
                ),
            ) ?: error("Не удалось найти TTF-шрифт для генерации PDF спецификации.")
            val boldFont = loadFont(
                document = document,
                candidates = listOf(
                    File("C:/Windows/Fonts/arialbd.ttf"),
                    File("C:/Windows/Fonts/ARIALNB.TTF"),
                    File("theme/src/commonMain/composeResources/font/MontserratAlternates-Bold.ttf"),
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

    private fun loadFont(
        document: PDDocument,
        candidates: List<File>,
    ): PDType0Font? {
        val fontFile = candidates.firstOrNull(File::exists) ?: return null
        return PDType0Font.load(document, fontFile)
    }
}

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

        drawSection("Описание", specification.description)
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
        drawSection("Аллергены", specification.allergens)
        drawSection("Информация о ГМО", specification.gmoInfo)

        stream.close()
    }

    private fun drawSection(
        title: String,
        content: String,
    ) {
        if (content.isBlank()) return
        drawParagraph(title, boldFont, 12f, 16f)
        drawParagraph(content, regularFont, 10.5f, 14f)
        spacer(8f)
    }

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

    private fun newPage() {
        page = PDPage(PDRectangle.A4)
        document.addPage(page)
        stream = PDPageContentStream(document, page)
        y = pageHeight - margin
    }
}
