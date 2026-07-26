package ru.pavlig43.money.internal.model

import ru.pavlig43.datetime.period.dateTime.DTPeriod

object MoneyReportCalculator {
    fun calculate(
        entries: List<MoneyReportEntry>,
        period: DTPeriod,
    ): MoneyReportData {
        val sorted = entries
            .asSequence()
            .filter { it.amount > 0 && it.occurredAt <= period.end }
            .sortedWith(
                compareBy<MoneyReportEntry>(
                    { it.occurredAt },
                    { it.operationGroupKey ?: it.sourceKey },
                    { if (it.sourceKey.startsWith("expense:")) 1 else 0 },
                    { it.sourceKey },
                ),
            )
            .toList()

        var runningBalance = 0L
        var openingBalance = 0L
        var received = 0L
        var spent = 0L
        val rows = mutableListOf<MoneyMovementRow>()

        sorted.forEach { entry ->
            runningBalance += entry.signedAmount()
            if (entry.occurredAt < period.start) {
                openingBalance = runningBalance
            } else {
                when (entry.kind) {
                    MoneyReportEntryKind.INCOME -> received += entry.amount
                    MoneyReportEntryKind.EXPENSE -> spent += entry.amount
                }
                rows += MoneyMovementRow(entry, runningBalance)
            }
        }

        val incomeByCategory = categoryTotals(rows, MoneyReportEntryKind.INCOME)
        val expensesByCategory = categoryTotals(rows, MoneyReportEntryKind.EXPENSE)
        val netChange = received - spent
        return MoneyReportData(
            summary = MoneySummary(
                openingBalance = openingBalance,
                received = received,
                spent = spent,
                netChange = netChange,
                closingBalance = openingBalance + netChange,
            ),
            incomeByCategory = incomeByCategory,
            expensesByCategory = expensesByCategory,
            movementRows = rows,
        )
    }

    private fun categoryTotals(
        rows: List<MoneyMovementRow>,
        kind: MoneyReportEntryKind,
    ): List<MoneyCategoryTotal> = rows
        .asSequence()
        .map(MoneyMovementRow::entry)
        .filter { it.kind == kind }
        .groupBy(MoneyReportEntry::category)
        .map { (category, items) ->
            MoneyCategoryTotal(category, items.sumOf(MoneyReportEntry::amount))
        }
        .sortedByDescending(MoneyCategoryTotal::amount)
}

fun MoneyReportEntry.signedAmount(): Long = when (kind) {
    MoneyReportEntryKind.INCOME -> amount
    MoneyReportEntryKind.EXPENSE -> -amount
}
