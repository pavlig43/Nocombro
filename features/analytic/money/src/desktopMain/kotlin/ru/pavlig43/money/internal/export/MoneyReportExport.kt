package ru.pavlig43.money.internal.export

import ru.pavlig43.money.internal.model.MoneyMovementRow
import ru.pavlig43.money.internal.model.displayName
import ru.pavlig43.money.internal.model.signedAmount
import ru.pavlig43.tablecore.export.ExcelColumn
import ru.pavlig43.tablecore.export.ExportCellValue

internal data class MoneyReportExportData(
    val columns: List<ExcelColumn>,
    val highlightedRowIndexes: Set<Int>,
)

internal fun buildMoneyReportExport(rows: List<MoneyMovementRow>): MoneyReportExportData {
    val linkedGroupKeys = rows
        .mapNotNull { it.entry.operationGroupKey }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys

    fun groupLabel(row: MoneyMovementRow): ExportCellValue {
        val entry = row.entry
        return if (entry.operationGroupKey in linkedGroupKeys) {
            ExportCellValue.Text(entry.operationGroupLabel.orEmpty())
        } else {
            ExportCellValue.Empty
        }
    }

    fun text(value: String) = ExportCellValue.Text(value)
    fun money(value: Long) = ExportCellValue.Number(value / KOPECKS_IN_RUBLE)

    return MoneyReportExportData(
        columns = listOf(
            ExcelColumn("Связь", rows.map(::groupLabel)),
            ExcelColumn("Дата", rows.map { ExportCellValue.DateTime(it.entry.occurredAt) }),
            ExcelColumn("Источник", rows.map { text(it.entry.sourceLabel) }),
            ExcelColumn("Вид", rows.map { text(it.entry.kind.displayName()) }),
            ExcelColumn("Категория", rows.map { text(it.entry.category.displayName()) }),
            ExcelColumn("Контрагент", rows.map { text(it.entry.counterparty) }),
            ExcelColumn("Комментарий", rows.map { text(it.entry.comment) }),
            ExcelColumn("Сумма, ₽", rows.map { money(it.entry.signedAmount()) }),
            ExcelColumn("Остаток, ₽", rows.map { money(it.runningBalance) }),
        ),
        highlightedRowIndexes = rows.indices
            .filterTo(mutableSetOf()) { index ->
                rows[index].entry.operationGroupKey in linkedGroupKeys
            },
    )
}

private const val KOPECKS_IN_RUBLE = 100.0
