package ru.pavlig43.money.internal.model

import ru.pavlig43.database.data.money.MoneyMovement
import ru.pavlig43.database.data.money.MoneyMovementCategory
import ru.pavlig43.database.data.money.MoneyMovementKind
import ru.pavlig43.datetime.period.dateTime.DTPeriod

object MoneyReportCalculator {
    fun calculate(
        movements: List<MoneyMovement>,
        period: DTPeriod,
    ): MoneyReportData {
        val sorted = movements
            .asSequence()
            .filter { it.deletedAt == null && it.occurredAt <= period.end }
            .sortedWith(compareBy<MoneyMovement>({ it.occurredAt }, { it.syncId }))
            .toList()

        var runningBalance = 0L
        var openingBalance = 0L
        var received = 0L
        var spent = 0L
        var openingBalancesEntered = 0L
        val rows = mutableListOf<MoneyMovementRow>()

        sorted.forEach { movement ->
            runningBalance += globalImpact(movement)

            if (movement.occurredAt < period.start) {
                openingBalance = runningBalance
            } else {
                when (movement.kind) {
                    MoneyMovementKind.INCOME -> received += movement.amount
                    MoneyMovementKind.EXPENSE -> spent += movement.amount
                    MoneyMovementKind.OPENING_BALANCE -> {
                        openingBalancesEntered += globalImpact(movement)
                    }
                    MoneyMovementKind.TRANSFER -> Unit
                }
                rows += MoneyMovementRow(
                    movement = movement,
                    runningBalance = runningBalance,
                )
            }
        }

        val incomeByCategory = categoryTotals(rows, MoneyMovementKind.INCOME)
        val expensesByCategory = categoryTotals(rows, MoneyMovementKind.EXPENSE)

        val netChange = received - spent
        return MoneyReportData(
            summary = MoneySummary(
                openingBalance = openingBalance,
                received = received,
                spent = spent,
                netChange = netChange,
                closingBalance = openingBalance + netChange + openingBalancesEntered,
            ),
            incomeByCategory = incomeByCategory,
            expensesByCategory = expensesByCategory,
            movementRows = rows,
        )
    }

    private fun categoryTotals(
        rows: List<MoneyMovementRow>,
        kind: MoneyMovementKind,
    ): List<MoneyCategoryTotal> = rows
        .asSequence()
        .map(MoneyMovementRow::movement)
        .filter { it.kind == kind }
        .groupBy { it.category ?: MoneyMovementCategory.OTHER }
        .map { (category, items) ->
            MoneyCategoryTotal(category, items.sumOf(MoneyMovement::amount))
        }
        .sortedByDescending(MoneyCategoryTotal::amount)

    private fun globalImpact(movement: MoneyMovement): Long = when (movement.kind) {
        MoneyMovementKind.OPENING_BALANCE -> {
            if (movement.toAccountId != null) movement.amount else -movement.amount
        }
        MoneyMovementKind.INCOME -> movement.amount
        MoneyMovementKind.EXPENSE -> -movement.amount
        MoneyMovementKind.TRANSFER -> 0L
    }
}
