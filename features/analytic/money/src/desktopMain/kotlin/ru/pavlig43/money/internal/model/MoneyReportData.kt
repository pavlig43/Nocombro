package ru.pavlig43.money.internal.model

import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.money.MoneyMovementCategory

data class MoneyReportData(
    val summary: MoneySummary = MoneySummary(),
    val incomeByCategory: List<MoneyCategoryTotal> = emptyList(),
    val expensesByCategory: List<MoneyCategoryTotal> = emptyList(),
    val movementRows: List<MoneyMovementRow> = emptyList(),
)

data class MoneySummary(
    val openingBalance: Long = 0,
    val received: Long = 0,
    val spent: Long = 0,
    val netChange: Long = 0,
    val closingBalance: Long = 0,
)

data class MoneyCategoryTotal(
    val category: MoneyMovementCategory,
    val amount: Long,
)

data class MoneyReportEntry(
    val sourceKey: String,
    val sourceLabel: String,
    val kind: MoneyReportEntryKind,
    val category: MoneyMovementCategory,
    val amount: Long,
    val occurredAt: LocalDateTime,
    val counterparty: String,
    val comment: String,
    val operationGroupKey: String? = null,
    val operationGroupLabel: String? = null,
)

enum class MoneyReportEntryKind {
    INCOME,
    EXPENSE,
}

data class MoneyMovementRow(
    val entry: MoneyReportEntry,
    val runningBalance: Long,
)
