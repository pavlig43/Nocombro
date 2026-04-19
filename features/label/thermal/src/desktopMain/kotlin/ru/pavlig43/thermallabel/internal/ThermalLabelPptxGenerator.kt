package ru.pavlig43.thermallabel.internal

import java.io.ByteArrayOutputStream
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.write
import kotlinx.datetime.format
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFShape
import org.apache.poi.xslf.usermodel.XSLFTextShape
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties
import ru.pavlig43.datetime.dateFormat
import ru.pavlig43.thermallabel.api.model.ThermalLabelGenerationRequest

/**
 * Генерирует PPTX-файл термоэтикетки на основе шаблона из resources.
 *
 * Класс подставляет текст в именованные shape'ы первого слайда, стараясь
 * сохранить форматирование, заданное в шаблоне PowerPoint.
 */
internal class ThermalLabelPptxGenerator {

    suspend fun generate(
        request: ThermalLabelGenerationRequest,
    ): String {
        val outputFile = buildOutputFile()

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
                replaceShapeText(slide.shapes, COMPOSITION_SHAPE_NAME, prefixed("Состав:", request.composition))
                replaceShapeText(slide.shapes, DOSAGE_SHAPE_NAME, prefixed("Дозировка:", request.dosage))
                replaceShapeText(slide.shapes, STORAGE_SHAPE_NAME, request.storageText)
                replaceShapeText(slide.shapes, DATE_SHAPE_NAME, "$DATE_LABEL ${request.date.format(dateFormat)}")
                replaceShapeText(slide.shapes, MASS_SHAPE_NAME, "Масса: ${request.massText.trim()} кг")

                val bytes = ByteArrayOutputStream().use { output ->
                    slideShow.write(output)
                    output.toByteArray()
                }
                outputFile.parent()?.createDirectories()
                outputFile write bytes
            }
        }

        return outputFile.absolutePath()
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

    private fun buildOutputFile(): PlatformFile {
        val outputDir = getThermalLabelsTempDirectory()
        return PlatformFile(outputDir, "thermal-label.pptx")
    }
}

/**
 * Пересобирает текстовый shape, сохраняя базовые настройки абзаца и run'а из шаблона.
 */
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

/**
 * Собирает строку вида `Заголовок: значение`, а если значение пустое,
 * оставляет только сам заголовок.
 */
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

/**
 * Возвращает временный каталог, в который Nocombro складывает сгенерированные термочеки.
 *
 * Каталог живет внутри системной temp-директории текущего пользователя и не считается
 * постоянным managed storage приложения.
 */
private fun getThermalLabelsTempDirectory(): PlatformFile {
    val tempRoot = PlatformFile(System.getProperty("java.io.tmpdir"))
    return PlatformFile(tempRoot, "Nocombro/thermal-labels")
}

private data class ProductTitleParts(
    val headerText: String,
    val nameText: String,
)

/**
 * Выделяет из имени продукта служебный префикс `КПД` и, если он найден,
 * выносит его в отдельный верхний заголовок этикетки.
 */
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
private const val DATE_LABEL = "Дата производства:"
private const val KPD_PREFIX = "КПД"
private const val COMPLEX_FOOD_ADDITIVE_TEXT = "Комплексная пищевая добавка"
