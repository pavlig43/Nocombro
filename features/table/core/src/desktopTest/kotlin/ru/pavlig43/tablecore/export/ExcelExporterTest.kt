package ru.pavlig43.tablecore.export

import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

class ExcelExporterTest : DesktopMainDispatcherFunSpec({
    test("highlighted rows use one visible Excel style") {
        val workbook = createWorkbookBytes(
            columns = listOf(
                ExcelColumn(
                    header = "Источник",
                    values = listOf(
                        ExportCellValue.Text("Закупка №22"),
                        ExportCellValue.Text("Трата №30"),
                    ),
                ),
                ExcelColumn(
                    header = "Дата",
                    values = listOf(
                        ExportCellValue.DateTime(LocalDateTime(2026, 7, 12, 21, 56)),
                        ExportCellValue.DateTime(LocalDateTime(2026, 7, 13, 10, 0)),
                    ),
                ),
            ),
            highlightedRowIndexes = setOf(0),
        )

        val entries = workbook.zipEntries()
        val quote = '"'
        entries.getValue("xl/styles.xml") shouldContain
            ("<fills count=" + quote + "5" + quote + ">")
        entries.getValue("xl/worksheets/sheet1.xml") shouldContain
            ("<c r=" + quote + "A2" + quote + " s=" + quote + "8" + quote)
        entries.getValue("xl/worksheets/sheet1.xml") shouldContain
            ("<c r=" + quote + "B2" + quote + " s=" + quote + "10" + quote)
        entries.getValue("xl/worksheets/sheet1.xml") shouldContain
            ("<c r=" + quote + "A3" + quote + " s=" + quote + "3" + quote)
    }
})

private fun ByteArray.zipEntries(): Map<String, String> = buildMap {
    ZipInputStream(ByteArrayInputStream(this@zipEntries)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            put(entry.name, zip.readBytes().toString(Charsets.UTF_8))
        }
    }
}
