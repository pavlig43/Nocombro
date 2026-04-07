package ru.pavlig43.tablecore.export

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.openFileSaver
import io.github.vinceglb.filekit.dialogs.openFileWithDefaultApplication
import io.github.vinceglb.filekit.write
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime as JavaLocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Уже подготовленная колонка для Excel-экспортера:
 * текст заголовка плюс типизированные значения всех строк.
 */
data class ExcelColumn(
    val header: String,
    val values: List<ExportCellValue>,
)

/**
 * Внешняя точка входа в Excel-экспорт.
 *
 * Здесь только выбор файла и запись готового `.xlsx`-пакета на диск.
 */
suspend fun exportExcelFile(
    suggestedFileName: String,
    columns: List<ExcelColumn>,
): Result<Unit> =
    runCatching {
        val saveFile = FileKit.openFileSaver(
            suggestedName = suggestedFileName,
            extension = "xlsx",
        ) ?: return@runCatching Unit

        saveFile.write(createWorkbookBytes(columns))
        FileKit.openFileWithDefaultApplication(saveFile)
    }

/**
 * Собирает минимальный `.xlsx`-пакет в памяти.
 *
 * `.xlsx` — это обычный zip-архив с набором XML-файлов.
 * Здесь мы создаем только те части, которых Excel достаточно для открытия таблицы.
 */
private fun createWorkbookBytes(columns: List<ExcelColumn>): ByteArray {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
        zip.writeEntry("[Content_Types].xml", contentTypesXml())
        zip.writeEntry("_rels/.rels", rootRelsXml())
        zip.writeEntry("docProps/app.xml", appXml())
        zip.writeEntry("docProps/core.xml", coreXml())
        zip.writeEntry("xl/workbook.xml", workbookXml())
        zip.writeEntry("xl/styles.xml", stylesXml())
        zip.writeEntry("xl/_rels/workbook.xml.rels", workbookRelsXml())
        zip.writeEntry("xl/worksheets/sheet1.xml", worksheetXml(columns))
    }
    return output.toByteArray()
}

/**
 * Маленький helper, чтобы не дублировать запись XML-файлов в zip.
 */
private fun ZipOutputStream.writeEntry(
    path: String,
    content: String,
) {
    putNextEntry(ZipEntry(path))
    write(content.toByteArray(Charsets.UTF_8))
    closeEntry()
}

/**
 * Описывает состав `.xlsx`-пакета: какие части в нем есть и какого они типа.
 */
private fun contentTypesXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
      <Default Extension="xml" ContentType="application/xml"/>
      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
      <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
      <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
      <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
    </Types>
    """.trimIndent()

/**
 * Корневые связи zip-пакета: workbook и служебные метаданные.
 */
private fun rootRelsXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
      <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
    </Relationships>
    """.trimIndent()

/**
 * Служебные расширенные свойства документа.
 */
private fun appXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
      <Application>Nocombro</Application>
    </Properties>
    """.trimIndent()

/**
 * Базовые метаданные файла: автор, время создания и изменения.
 */
private fun coreXml(): String {
    val now = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <dc:creator>Nocombro</dc:creator>
          <cp:lastModifiedBy>Nocombro</cp:lastModifiedBy>
          <dcterms:created xsi:type="dcterms:W3CDTF">$now</dcterms:created>
          <dcterms:modified xsi:type="dcterms:W3CDTF">$now</dcterms:modified>
        </cp:coreProperties>
    """.trimIndent()
}

/**
 * Workbook здесь один и содержит один лист `Export`.
 */
private fun workbookXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
      <sheets>
        <sheet name="Export" sheetId="1" r:id="rId1"/>
      </sheets>
    </workbook>
    """.trimIndent()

/**
 * Связи workbook: сам лист и таблица стилей.
 */
private fun workbookRelsXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    </Relationships>
    """.trimIndent()

/**
 * Описывает все визуальные стили Excel:
 * шрифты, фоны, границы и форматы дат/даты-времени.
 */
private fun stylesXml(): String =
    """
    <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
    <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
      <numFmts count="2">
        <numFmt numFmtId="164" formatCode="dd.mm.yyyy"/>
        <numFmt numFmtId="165" formatCode="dd.mm.yyyy hh:mm"/>
      </numFmts>
      <fonts count="2">
        <font>
          <sz val="11"/>
          <name val="Calibri"/>
        </font>
        <font>
          <b/>
          <sz val="13"/>
          <name val="Calibri"/>
        </font>
      </fonts>
      <fills count="4">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFDCE6F1"/><bgColor indexed="64"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFF7F9FC"/><bgColor indexed="64"/></patternFill></fill>
      </fills>
      <borders count="2">
        <border><left/><right/><top/><bottom/><diagonal/></border>
        <border>
          <left style="medium"><color rgb="FF9AA5B1"/></left>
          <right style="medium"><color rgb="FF9AA5B1"/></right>
          <top style="medium"><color rgb="FF9AA5B1"/></top>
          <bottom style="medium"><color rgb="FF9AA5B1"/></bottom>
          <diagonal/>
        </border>
      </borders>
      <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
      </cellStyleXfs>
      <cellXfs count="7">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1">
          <alignment horizontal="center" vertical="center"/>
        </xf>
        <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
        <xf numFmtId="0" fontId="0" fillId="3" borderId="1" xfId="0" applyFill="1" applyBorder="1"/>
        <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
        <xf numFmtId="164" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFill="1" applyBorder="1"/>
        <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1"/>
        <xf numFmtId="165" fontId="0" fillId="3" borderId="1" xfId="0" applyNumberFormat="1" applyFill="1" applyBorder="1"/>
      </cellXfs>
      <cellStyles count="1">
        <cellStyle name="Normal" xfId="0" builtinId="0"/>
      </cellStyles>
    </styleSheet>
    """.trimIndent()

/**
 * Генерирует содержимое первого листа.
 *
 * Здесь задаются размеры таблицы, freeze header, ширины колонок и сами строки данных.
 * Чередование строк задается стилями, поэтому "зебра" остается только визуальным слоем,
 * а типы ячеек для Excel сохраняются корректными.
 */
private fun worksheetXml(columns: List<ExcelColumn>): String {
    val dataRows = columns.maxOfOrNull { it.values.size } ?: 0
    val rowCount = dataRows + 1
    val lastColumnRef = if (columns.isEmpty()) "A" else columnRef(columns.lastIndex)

    val rowsXml = buildString {
        append("""<row r="1">""")
        columns.forEachIndexed { index, column ->
            append(headerCell("${columnRef(index)}1", column.header))
        }
        append("</row>")

        for (rowIndex in 0 until dataRows) {
            val excelRow = rowIndex + 2
            val zebra = rowIndex % 2 == 1
            append("""<row r="$excelRow">""")
            columns.forEachIndexed { index, column ->
                append(
                    cellXml(
                        ref = "${columnRef(index)}$excelRow",
                        value = column.values.getOrElse(rowIndex) { ExportCellValue.Empty },
                        zebra = zebra,
                    ),
                )
            }
            append("</row>")
        }
    }

    val colsXml = buildString {
        if (columns.isNotEmpty()) {
            append("<cols>")
            columns.forEachIndexed { index, column ->
                append("""<col min="${index + 1}" max="${index + 1}" width="${excelWidth(column)}" customWidth="1"/>""")
            }
            append("</cols>")
        }
    }

    return """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <dimension ref="A1:$lastColumnRef$rowCount"/>
          <sheetViews>
            <sheetView workbookViewId="0">
              <pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/>
            </sheetView>
          </sheetViews>
          <sheetFormatPr defaultRowHeight="15"/>
          $colsXml
          <sheetData>$rowsXml</sheetData>
        </worksheet>
    """.trimIndent()
}

/**
 * Маршрутизатор по типу значения.
 *
 * На этом этапе решаем, какой XML ячейки должен попасть в `sheet1.xml`.
 */
private fun cellXml(
    ref: String,
    value: ExportCellValue,
    zebra: Boolean,
): String =
    when (value) {
        ExportCellValue.Empty -> blankCell(ref, zebra)
        is ExportCellValue.Text -> textCell(ref, value.value, zebra)
        is ExportCellValue.Number -> numericCell(ref, value.value, styleIndex = null, zebra = zebra)
        is ExportCellValue.BooleanValue -> booleanCell(ref, value.value, zebra)
        is ExportCellValue.Date -> numericCell(
            ref,
            excelSerialDate(value.value),
            styleIndex = if (zebra) 5 else 4,
            zebra = zebra,
        )
        is ExportCellValue.DateTime -> numericCell(
            ref,
            excelSerialDateTime(value.value),
            styleIndex = if (zebra) 7 else 6,
            zebra = zebra,
        )
    }

/**
 * Заголовок таблицы всегда пишем как `inlineStr` и отдельным стилем header.
 */
private fun headerCell(
    ref: String,
    value: String,
): String = """<c r="$ref" s="1" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>"""

/**
 * Обычная текстовая ячейка.
 */
private fun textCell(
    ref: String,
    value: String,
    zebra: Boolean,
): String {
    val style = if (zebra) 3 else 2
    return """<c r="$ref" s="$style" t="inlineStr"><is><t xml:space="preserve">${escapeXml(value)}</t></is></c>"""
}

/**
 * Числа пишем как numeric cell, а не как текст.
 *
 * Благодаря этому Excel сам показывает запятую или точку по локали пользователя.
 */
private fun numericCell(
    ref: String,
    value: Double,
    styleIndex: Int?,
    zebra: Boolean,
): String {
    val resolvedStyle = styleIndex ?: if (zebra) 3 else 2
    return """<c r="$ref" s="$resolvedStyle"><v>${doubleToExcelString(value)}</v></c>"""
}

/**
 * Boolean в Excel храним отдельным типом ячейки `b`.
 */
private fun booleanCell(
    ref: String,
    value: Boolean,
    zebra: Boolean,
): String {
    val style = if (zebra) 3 else 2
    return """<c r="$ref" s="$style" t="b"><v>${if (value) 1 else 0}</v></c>"""
}

/**
 * Пустая ячейка все равно получает стиль, чтобы не терять границы и "зебру".
 */
private fun blankCell(
    ref: String,
    zebra: Boolean,
): String {
    val style = if (zebra) 3 else 2
    return """<c r="$ref" s="$style"/>"""
}

/**
 * Простейшая эвристика ширины колонки:
 * берем максимум между header и длиной отображаемого значения.
 */
private fun excelWidth(column: ExcelColumn): Int {
    val maxLength = maxOf(column.header.length, column.values.maxOfOrNull { displayLength(it) } ?: 0)
    return maxLength.coerceIn(10, 60) + 2
}

/**
 * Используется только для расчета ширины, а не для самой сериализации в Excel.
 */
private fun displayLength(value: ExportCellValue): Int =
    when (value) {
        ExportCellValue.Empty -> 0
        is ExportCellValue.Text -> value.value.length
        is ExportCellValue.Number -> doubleToExcelString(value.value).length
        is ExportCellValue.BooleanValue -> if (value.value) 4 else 5
        is ExportCellValue.Date -> 10
        is ExportCellValue.DateTime -> 16
    }

/**
 * Переводит календарную дату в Excel serial value.
 *
 * Excel хранит даты как серийные числа, а не как отформатированные строки.
 */
private fun excelSerialDate(value: LocalDate): Double {
    val javaDate = java.time.LocalDate.of(value.year, value.month.ordinal + 1, value.day)
    val excelEpoch = java.time.LocalDate.of(1899, 12, 30)
    return java.time.temporal.ChronoUnit.DAYS.between(excelEpoch, javaDate).toDouble()
}

/**
 * Переводит дату-время в Excel serial value.
 *
 * Используется та же серийная модель, что и для даты, но с дробной частью дня.
 */
private fun excelSerialDateTime(value: LocalDateTime): Double {
    val javaDateTime = JavaLocalDateTime.of(
        value.year,
        value.month.ordinal + 1,
        value.day,
        value.hour,
        value.minute,
        value.second,
        value.nanosecond,
    )
    val excelEpoch = JavaLocalDateTime.of(1899, 12, 30, 0, 0)
    val duration = java.time.Duration.between(excelEpoch, javaDateTime)
    return duration.seconds / 86_400.0 + duration.nano / 86_400_000_000_000.0
}

/**
 * Убирает лишнюю `.0` у целых чисел, чтобы XML оставался чище и предсказуемее.
 */
private fun doubleToExcelString(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

/**
 * Переводит индекс `0,1,2...` в Excel-обозначения колонок `A,B,C...AA...`.
 */
private fun columnRef(index: Int): String {
    var current = index
    val builder = StringBuilder()
    do {
        val remainder = current % 26
        builder.append(('A'.code + remainder).toChar())
        current = current / 26 - 1
    } while (current >= 0)
    return builder.reverse().toString()
}

/**
 * Минимальное экранирование спецсимволов для безопасной вставки текста в XML.
 */
private fun escapeXml(value: String): String =
    buildString(value.length) {
        value.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(char)
            }
        }
    }
