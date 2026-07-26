package ru.pavlig43.money.internal.export

import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.money.internal.model.MoneyMovementRow
import ru.pavlig43.money.internal.model.MoneyReportEntry
import ru.pavlig43.money.internal.model.MoneyReportEntryKind
import ru.pavlig43.tablecore.export.ExportCellValue
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

class MoneyReportExportTest : DesktopMainDispatcherFunSpec({
    test("linked rows get one group label and Excel highlight") {
        val at = LocalDateTime(2026, 7, 12, 21, 56)
        val rows = listOf(
            row(
                key = "purchase:22",
                label = "Закупка №22",
                category = MoneyMovementCategory.PURCHASE_PAYMENT,
                amount = 17_445_150,
                balance = -17_445_150,
                at = at,
                groupKey = "transaction:22",
                groupLabel = "Закупка №22",
            ),
            row(
                key = "expense:21",
                label = "Трата №21",
                category = MoneyMovementCategory.BUSINESS_EXPENSE,
                amount = 1_134_899,
                balance = -18_580_049,
                at = at,
                groupKey = "transaction:22",
                groupLabel = "Закупка №22",
            ),
            row(
                key = "expense:30",
                label = "Трата №30",
                category = MoneyMovementCategory.DIVIDEND,
                amount = 2_000_000,
                balance = -20_580_049,
                at = at,
            ),
        )

        val export = buildMoneyReportExport(rows)

        export.highlightedRowIndexes shouldBe setOf(0, 1)
        export.columns.map { it.header } shouldBe listOf(
            "Связь",
            "Дата",
            "Источник",
            "Вид",
            "Категория",
            "Контрагент",
            "Комментарий",
            "Сумма, ₽",
            "Остаток, ₽",
        )
        export.columns.first().values shouldBe listOf(
            ExportCellValue.Text("Закупка №22"),
            ExportCellValue.Text("Закупка №22"),
            ExportCellValue.Empty,
        )
        export.columns[7].values shouldBe listOf(
            ExportCellValue.Number(-174_451.5),
            ExportCellValue.Number(-11_348.99),
            ExportCellValue.Number(-20_000.0),
        )
    }
})

private fun row(
    key: String,
    label: String,
    category: MoneyMovementCategory,
    amount: Long,
    balance: Long,
    at: LocalDateTime,
    groupKey: String? = null,
    groupLabel: String? = null,
): MoneyMovementRow = MoneyMovementRow(
    entry = MoneyReportEntry(
        sourceKey = key,
        sourceLabel = label,
        kind = MoneyReportEntryKind.EXPENSE,
        category = category,
        amount = amount,
        occurredAt = at,
        counterparty = "",
        comment = "",
        operationGroupKey = groupKey,
        operationGroupLabel = groupLabel,
    ),
    runningBalance = balance,
)
