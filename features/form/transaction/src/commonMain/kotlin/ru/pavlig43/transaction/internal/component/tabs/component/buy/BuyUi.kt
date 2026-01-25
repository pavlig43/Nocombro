package ru.pavlig43.transaction.internal.component.tabs.component.buy

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.emptyDate
import ru.pavlig43.tablecore.model.ITableUi

data class BuyUi(
    override val composeId: Int,
    val id: Int,
    val productId: Int = 0,
    val productName: String = "",
    val count: Int = 0,
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val dateBorn: LocalDate = emptyDate,
    val price: Int = 0,
    val comment: String = "",
) : ITableUi