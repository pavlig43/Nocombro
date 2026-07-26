package ru.pavlig43.money.internal.model

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

class MoneyReportCalculatorTest : DesktopMainDispatcherFunSpec({
    val start = LocalDateTime(2026, 7, 1, 0, 0)
    val end = LocalDateTime(2026, 7, 31, 23, 59, 59)
    val period = DTPeriod(start, end)

    test("empty source data has zero summary") {
        val report = MoneyReportCalculator.calculate(emptyList(), period)

        report.summary shouldBe MoneySummary()
        report.movementRows.shouldHaveSize(0)
    }

    test("recorded expense without income produces an allowed negative balance") {
        val report = MoneyReportCalculator.calculate(
            entries = listOf(
                entry(
                    key = "expense:1",
                    kind = MoneyReportEntryKind.EXPENSE,
                    category = MoneyMovementCategory.BUSINESS_EXPENSE,
                    amount = 15_000,
                    at = start,
                )
            ),
            period = period,
        )

        report.summary.spent shouldBe 15_000L
        report.summary.closingBalance shouldBe -15_000L
    }

    test("recorded sales purchases and expenses produce one unambiguous total") {
        val report = MoneyReportCalculator.calculate(
            entries = listOf(
                entry(
                    key = "sale:1",
                    kind = MoneyReportEntryKind.INCOME,
                    category = MoneyMovementCategory.SALE_PAYMENT,
                    amount = 200_000,
                    at = LocalDateTime(2026, 7, 5, 12, 0),
                ),
                entry(
                    key = "purchase:1",
                    kind = MoneyReportEntryKind.EXPENSE,
                    category = MoneyMovementCategory.PURCHASE_PAYMENT,
                    amount = 50_000,
                    at = LocalDateTime(2026, 7, 6, 12, 0),
                ),
                entry(
                    key = "expense:1",
                    kind = MoneyReportEntryKind.EXPENSE,
                    category = MoneyMovementCategory.DIVIDEND,
                    amount = 30_000,
                    at = LocalDateTime(2026, 7, 7, 12, 0),
                ),
            ),
            period = period,
        )

        report.summary shouldBe MoneySummary(
            openingBalance = 0,
            received = 200_000,
            spent = 80_000,
            netChange = 120_000,
            closingBalance = 120_000,
        )
        report.incomeByCategory.single().amount shouldBe 200_000L
        report.expensesByCategory.map { it.category to it.amount } shouldBe listOf(
            MoneyMovementCategory.PURCHASE_PAYMENT to 50_000L,
            MoneyMovementCategory.DIVIDEND to 30_000L,
        )
        report.movementRows.last().runningBalance shouldBe 120_000L
    }

    test("entries before and on period boundaries calculate opening and stable order") {
        val sameTime = LocalDateTime(2026, 7, 10, 10, 0)
        val report = MoneyReportCalculator.calculate(
            entries = listOf(
                entry(
                    key = "purchase:before",
                    kind = MoneyReportEntryKind.EXPENSE,
                    category = MoneyMovementCategory.PURCHASE_PAYMENT,
                    amount = 10_000,
                    at = LocalDateTime(2025, 1, 1, 0, 0),
                ),
                entry(
                    key = "expense:start",
                    kind = MoneyReportEntryKind.EXPENSE,
                    category = MoneyMovementCategory.BUSINESS_EXPENSE,
                    amount = 20_000,
                    at = start,
                ),
                entry(
                    key = "sale:b",
                    kind = MoneyReportEntryKind.INCOME,
                    category = MoneyMovementCategory.SALE_PAYMENT,
                    amount = 5_000,
                    at = sameTime,
                ),
                entry(
                    key = "sale:a",
                    kind = MoneyReportEntryKind.INCOME,
                    category = MoneyMovementCategory.SALE_PAYMENT,
                    amount = 3_000,
                    at = sameTime,
                ),
                entry(
                    key = "sale:end",
                    kind = MoneyReportEntryKind.INCOME,
                    category = MoneyMovementCategory.SALE_PAYMENT,
                    amount = 1_000,
                    at = end,
                ),
                entry(
                    key = "sale:after",
                    kind = MoneyReportEntryKind.INCOME,
                    category = MoneyMovementCategory.SALE_PAYMENT,
                    amount = 100_000,
                    at = LocalDateTime(2026, 8, 1, 0, 0),
                ),
            ),
            period = period,
        )

        report.summary.openingBalance shouldBe -10_000L
        report.summary.closingBalance shouldBe -21_000L
        report.incomeByCategory.single().amount shouldBe 9_000L
        report.movementRows.map { it.entry.sourceKey } shouldBe listOf(
            "expense:start", "sale:a", "sale:b", "sale:end"
        )
    }

    test("linked document is shown before its expense at the same time") {
        val sameTime = LocalDateTime(2026, 7, 12, 21, 56)
        val report = MoneyReportCalculator.calculate(
            entries = listOf(
                entry(
                    key = "expense:21",
                    kind = MoneyReportEntryKind.EXPENSE,
                    category = MoneyMovementCategory.BUSINESS_EXPENSE,
                    amount = 1_134_899,
                    at = sameTime,
                    groupKey = "transaction:22",
                    groupLabel = "Закупка №22",
                ),
                entry(
                    key = "purchase:22",
                    kind = MoneyReportEntryKind.EXPENSE,
                    category = MoneyMovementCategory.PURCHASE_PAYMENT,
                    amount = 17_445_150,
                    at = sameTime,
                    groupKey = "transaction:22",
                    groupLabel = "Закупка №22",
                ),
            ),
            period = period,
        )

        report.movementRows.map { it.entry.sourceKey } shouldBe listOf(
            "purchase:22",
            "expense:21",
        )
    }
})

private fun entry(
    key: String,
    kind: MoneyReportEntryKind,
    category: MoneyMovementCategory,
    amount: Long,
    at: LocalDateTime,
    groupKey: String? = null,
    groupLabel: String? = null,
): MoneyReportEntry = MoneyReportEntry(
    sourceKey = key,
    sourceLabel = key,
    kind = kind,
    category = category,
    amount = amount,
    occurredAt = at,
    counterparty = "",
    comment = "",
    operationGroupKey = groupKey,
    operationGroupLabel = groupLabel,
)
