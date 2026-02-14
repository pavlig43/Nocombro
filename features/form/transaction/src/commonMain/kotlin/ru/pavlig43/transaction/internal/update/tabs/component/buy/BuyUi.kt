package ru.pavlig43.transaction.internal.update.tabs.component.buy

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.emptyDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class BuyUi(
    override val composeId: Int,
    val id: Int,
    val productId: Int = 0,
    val productName: String = "",
    val count: Int = 0,
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val dateBorn: LocalDate = getCurrentLocalDate(),
    val price: Int = 0,
    val comment: String = "",
    val batchId: Int = 0,
    val movementId: Int = 0
) : IMultiLineTableUi