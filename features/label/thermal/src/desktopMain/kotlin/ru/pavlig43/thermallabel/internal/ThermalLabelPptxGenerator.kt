package ru.pavlig43.thermallabel.internal

import java.io.File
import kotlinx.datetime.LocalDate
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFShape
import org.apache.poi.xslf.usermodel.XSLFTextParagraph
import org.apache.poi.xslf.usermodel.XSLFTextRun
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties
import ru.pavlig43.thermallabel.api.model.ThermalLabelGenerationRequest
import ru.pavlig43.thermallabel.api.model.ThermalLabelSize

internal class ThermalLabelPptxGenerator {

    fun generate(
        request: ThermalLabelGenerationRequest,
    ): String {
        val templateFormat = request.size.templateFormat
        val outputFile = buildOutputFile(
            productName = request.productName,
            sizeSuffix = request.size.outputSuffix,
            dateText = formatDate(request.date),
        )

        val templateStream = javaClass.classLoader
            .getResourceAsStream(request.size.templateResourcePath)
            ?: error("Не найден шаблон `${request.size.title}`.")

        templateStream.use { input ->
            XMLSlideShow(input).use { slideShow ->
                val slide = slideShow.slides.firstOrNull()
                    ?: error("Шаблон `${request.size.title}` не содержит слайдов.")
                val titleParts = splitProductTitle(request.productName)

                replaceShapeText(slide.shapes, HEADER_SHAPE_NAME, titleParts.headerText)
                replaceShapeText(slide.shapes, NAME_SHAPE_NAME, titleParts.nameText)
                replaceShapeText(slide.shapes, COMPOSITION_SHAPE_NAME, templateFormat.compositionText(request.composition))
                replaceShapeText(slide.shapes, DOSAGE_SHAPE_NAME, templateFormat.dosageText(request.dosage))
                replaceShapeText(slide.shapes, STORAGE_SHAPE_NAME, request.storageText)
                replaceShapeText(slide.shapes, DATE_SHAPE_NAME, templateFormat.dateText(request.date))
                replaceShapeText(slide.shapes, MASS_SHAPE_NAME, templateFormat.massText(request.massText))

                outputFile.outputStream().use(slideShow::write)
            }
        }

        return outputFile.absolutePath
    }

    private fun replaceShapeText(
        shapes: List<XSLFShape>,
        shapeName: String,
        text: String,
    ) {
        val textShape = shapes
            .filterIsInstance<XSLFTextShape>()
            .firstOrNull { it.shapeName == shapeName }
            ?: error("В шаблоне не найден блок `$shapeName`.")

        replaceTextPreservingTemplateFormatting(textShape, text)
    }

    private fun buildOutputFile(
        productName: String,
        sizeSuffix: String,
        dateText: String,
    ): File {
        val outputDir = File(getNocombroAppDataDirectory(), "thermal-labels").apply { mkdirs() }
        val safeProductName = sanitizeFileSegment(productName.ifBlank { "label" })
        val safeDate = sanitizeFileSegment(dateText.ifBlank { formatDate(LocalDate(2000, 1, 1)) })
        return File(outputDir, "${safeProductName}_${sizeSuffix}_${safeDate}.pptx")
    }
}

private fun replaceTextPreservingTemplateFormatting(
    textShape: XSLFTextShape,
    text: String,
) {
    val paragraphTemplate = textShape.textParagraphs.firstOrNull()
    val runTemplate = paragraphTemplate?.textRuns?.firstOrNull()

    val paragraphProperties = paragraphTemplate
        ?.xmlObject
        ?.pPr
        ?.copy() as? CTTextParagraphProperties
    val endParagraphProperties = paragraphTemplate
        ?.xmlObject
        ?.endParaRPr
        ?.copy() as? CTTextCharacterProperties
    val runProperties = runTemplate
        ?.getRPr(false)
        ?.copy() as? CTTextCharacterProperties

    textShape.clearText()

    text
        .split('\n')
        .forEachIndexed { index, line ->
            val paragraph = if (index == 0) {
                textShape.addNewTextParagraph()
            } else {
                textShape.addNewTextParagraph()
            }

            paragraphProperties?.let {
                paragraph.xmlObject.setPPr(it)
            }

            val run = paragraph.addNewTextRun()
            runProperties?.let {
                run.getRPr(true).set(it)
            }
            run.setText(line)

            endParagraphProperties?.let {
                paragraph.xmlObject.setEndParaRPr(it)
            }
        }
}

private fun prefixed(
    prefix: String,
    value: String,
): String {
    val trimmed = value.trim()
    return if (trimmed.isBlank()) {
        prefix
    } else {
        "$prefix $trimmed"
    }
}

private fun normalizeMass(
    raw: String,
): String {
    return raw
        .trim()
        .removeSuffix("кг")
        .removeSuffix("kg")
        .trim()
        .ifBlank { "3" }
}

private fun sanitizeFileSegment(
    raw: String,
): String {
    return raw
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(Regex("\\s+"), "_")
}

private fun getNocombroAppDataDirectory(): File {
    val baseDir = System.getenv("APPDATA")
        ?.takeIf(String::isNotBlank)
        ?.let(::File)
        ?: File(System.getProperty("user.home"))
    return File(baseDir, "Nocombro").apply { mkdirs() }
}

private fun formatDate(date: LocalDate): String = "%02d.%02d.%04d".format(date.day, date.month.ordinal + 1, date.year)

private val ThermalLabelSize.templateFormat: ThermalLabelTemplateFormat
    get() = when (this) {
        ThermalLabelSize.SIZE_75_120 -> ThermalLabelTemplateFormat(
            dateLabel = "Дата производства:",
        )
        ThermalLabelSize.SIZE_100_150 -> ThermalLabelTemplateFormat(
            dateLabel = "Дата производства:",
        )
    }

private data class ThermalLabelTemplateFormat(
    val dateLabel: String,
) {
    fun compositionText(
        composition: String,
    ): String = prefixed("Состав:", composition)

    fun dosageText(
        dosage: String,
    ): String = prefixed("Дозировка:", dosage)

    fun dateText(
        date: LocalDate,
    ): String = "$dateLabel ${formatDate(date)}"

    fun massText(
        massText: String,
    ): String = "Масса: ${normalizeMass(massText)} кг"
}

private data class ProductTitleParts(
    val headerText: String,
    val nameText: String,
)

private fun splitProductTitle(
    productName: String,
): ProductTitleParts {
    val trimmedName = productName.trim()
    val compactName = trimmedName.replace(Regex("\\s+"), " ")

    return if (compactName.startsWith(KPD_PREFIX, ignoreCase = true)) {
        val labelName = compactName
            .substring(KPD_PREFIX.length)
            .trim()
            .ifBlank { trimmedName }
        ProductTitleParts(
            headerText = COMPLEX_FOOD_ADDITIVE_TEXT,
            nameText = labelName,
        )
    } else {
        ProductTitleParts(
            headerText = "",
            nameText = trimmedName,
        )
    }
}

private const val HEADER_SHAPE_NAME = "TextBox 2"
private const val NAME_SHAPE_NAME = "TextBox 10"
private const val COMPOSITION_SHAPE_NAME = "TextBox 3"
private const val DOSAGE_SHAPE_NAME = "TextBox 4"
private const val STORAGE_SHAPE_NAME = "TextBox 5"
private const val DATE_SHAPE_NAME = "TextBox 7"
private const val MASS_SHAPE_NAME = "TextBox 9"
private const val KPD_PREFIX = "КПД"
private const val COMPLEX_FOOD_ADDITIVE_TEXT = "Комплексная пищевая добавка"
