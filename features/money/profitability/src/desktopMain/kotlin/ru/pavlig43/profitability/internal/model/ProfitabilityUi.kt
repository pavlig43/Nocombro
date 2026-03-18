package ru.pavlig43.profitability.internal.model

data class ProfitabilityUi(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val revenue: Int,
    val expenses: Int,
    val expensesOnOneKg: Int,
    val profit: Int,
    val margin:Double,
    val profitability: Double
)
