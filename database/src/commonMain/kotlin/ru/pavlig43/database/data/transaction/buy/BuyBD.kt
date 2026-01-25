package ru.pavlig43.database.data.transaction.buy

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.CollectionObject

data class BuyBD(
    val productName: String,
    val count: Int,
    val declarationName: String,
    val vendorName: String,
    val dateBorn: LocalDate,
    val price: Int,
    val comment: String,
    override val id: Int
): CollectionObject