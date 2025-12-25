package ru.pavlig43.database.data.transaction.buy

import kotlinx.datetime.LocalDate

data class BuyBaseBD(
    val batchId: Int,
    val productName: String,
    val declarationName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val price: Int,
    val comment: String,
)