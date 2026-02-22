package ru.pavlig43.transaction.internal.update.tabs.component.sale

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class SaleUi(
    override val composeId: Int,
    val id: Int,
    val productName: String = "",
    val batchId: Int = 0,
    val count: Int = 0,
    val price: Int = 0,
    val vendorName: String = "",
    val comment: String = "",
    val movementId: Int = 0
) : IMultiLineTableUi {
    val sum: Int
        get() = (count.toLong() * price / 1000).toInt()
}
