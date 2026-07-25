package ru.pavlig43.money.internal.model

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.MoneyMovement
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.datetime.period.dateTime.DTPeriod
import ru.pavlig43.testkit.DesktopMainDispatcherFunSpec

class MoneyReportCalculatorTest : DesktopMainDispatcherFunSpec({
    val start = LocalDateTime(2026, 7, 1, 0, 0)
    val end = LocalDateTime(2026, 7, 31, 23, 59, 59)
    val period = DTPeriod(start, end)

    test("empty journal has zero summary") {
        val report = MoneyReportCalculator.calculate(emptyList(), period)

        report.summary shouldBe MoneySummary()
        report.movementRows.shouldHaveSize(0)
    }

    test("expense without an opening balance produces an allowed negative balance") {
        val report = MoneyReportCalculator.calculate(
            movements = listOf(
                movement(
                    kind = MoneyMovementKind.EXPENSE,
                    amount = 15_000,
                    at = start,
                    from = 1,
                    category = MoneyMovementCategory.BUSINESS_EXPENSE,
                    syncId = "expense-only",
                )
            ),
            period = period,
        )

        report.summary.spent shouldBe 15_000L
        report.summary.closingBalance shouldBe -15_000L
    }

    test("old opening balance and transfer remain compatible with the common total") {
        val movements = listOf(
            movement(
                kind = MoneyMovementKind.OPENING_BALANCE,
                amount = 100_000_000,
                at = start,
                to = 1,
                syncId = "1",
            ),
            movement(
                kind = MoneyMovementKind.INCOME,
                amount = 20_000_000,
                at = LocalDateTime(2026, 7, 5, 12, 0),
                to = 1,
                category = MoneyMovementCategory.SALE_PAYMENT,
                syncId = "2",
            ),
            movement(
                kind = MoneyMovementKind.EXPENSE,
                amount = 5_000_000,
                at = LocalDateTime(2026, 7, 6, 12, 0),
                from = 1,
                category = MoneyMovementCategory.BUSINESS_EXPENSE,
                syncId = "3",
            ),
            movement(
                kind = MoneyMovementKind.TRANSFER,
                amount = 30_000_000,
                at = LocalDateTime(2026, 7, 7, 12, 0),
                from = 1,
                to = 2,
                syncId = "4",
            ),
        )

        val report = MoneyReportCalculator.calculate(movements, period)

        report.summary shouldBe MoneySummary(
            openingBalance = 0,
            received = 20_000_000,
            spent = 5_000_000,
            netChange = 15_000_000,
            closingBalance = 115_000_000,
        )
        report.incomeByCategory.single().amount shouldBe 20_000_000L
        report.incomeByCategory.single().category shouldBe MoneyMovementCategory.SALE_PAYMENT
        report.expensesByCategory.single().amount shouldBe 5_000_000L
        report.movementRows.last().runningBalance shouldBe 115_000_000L
    }

    test("movements before and on period boundaries calculate opening and closing balances") {
        val sameTime = LocalDateTime(2026, 7, 10, 10, 0)
        val movements = listOf(
            movement(
                kind = MoneyMovementKind.INCOME,
                amount = 10_000,
                at = LocalDateTime(2025, 1, 1, 0, 0),
                to = 1,
                category = MoneyMovementCategory.OWNER_DEPOSIT,
                syncId = "before",
            ),
            movement(
                kind = MoneyMovementKind.EXPENSE,
                amount = 20_000,
                at = start,
                from = 1,
                category = MoneyMovementCategory.OTHER,
                syncId = "start",
            ),
            movement(
                kind = MoneyMovementKind.INCOME,
                amount = 5_000,
                at = sameTime,
                to = 1,
                category = MoneyMovementCategory.REFUND,
                syncId = "b",
            ),
            movement(
                kind = MoneyMovementKind.INCOME,
                amount = 3_000,
                at = sameTime,
                to = 1,
                category = MoneyMovementCategory.REFUND,
                syncId = "a",
            ),
            movement(
                kind = MoneyMovementKind.INCOME,
                amount = 1_000,
                at = end,
                to = 1,
                category = MoneyMovementCategory.OTHER,
                syncId = "end",
            ),
            movement(
                kind = MoneyMovementKind.INCOME,
                amount = 100_000,
                at = LocalDateTime(2026, 8, 1, 0, 0),
                to = 1,
                category = MoneyMovementCategory.OTHER,
                syncId = "after",
            ),
        )

        val report = MoneyReportCalculator.calculate(movements, period)

        report.summary.openingBalance shouldBe 10_000L
        report.summary.closingBalance shouldBe -1_000L
        report.incomeByCategory.map { it.category to it.amount } shouldBe listOf(
            MoneyMovementCategory.REFUND to 8_000L,
            MoneyMovementCategory.OTHER to 1_000L,
        )
        report.movementRows.map { it.movement.syncId } shouldBe listOf("start", "a", "b", "end")
    }
})

private fun movement(
    kind: MoneyMovementKind,
    amount: Long,
    at: LocalDateTime,
    from: Int? = null,
    to: Int? = null,
    category: MoneyMovementCategory? = null,
    syncId: String,
): MoneyMovement = MoneyMovement(
    kind = kind,
    category = category,
    amount = amount,
    occurredAt = at,
    fromAccountId = from,
    toAccountId = to,
    syncId = syncId,
)
