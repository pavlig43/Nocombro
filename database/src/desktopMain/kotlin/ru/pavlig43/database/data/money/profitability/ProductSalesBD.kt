package ru.pavlig43.database.data.money.profitability

data class ProductSalesBD(
    val productId: Int,
    val productName: String,
    val quantity: Int,
    val revenue: Int
)
