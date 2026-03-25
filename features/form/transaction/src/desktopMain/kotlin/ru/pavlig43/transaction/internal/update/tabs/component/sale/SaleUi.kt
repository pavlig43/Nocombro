package ru.pavlig43.transaction.internal.update.tabs.component.sale

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData2
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.tablecore.model.IMultiLineTableUi

@Suppress("MagicNumber")
data class SaleUi(
    override val composeId: Int,
    val id: Int,
    val productId: Int = 0,
    val productName: String = "",
    val batchId: Int = 0,
    val count: DecimalData3 = DecimalData3(0),
    val vendorName: String = "",
    val dateBorn: LocalDate = getCurrentLocalDate(),
    val clientName: String = "",
    val clientId: Int = 0,
    val price: DecimalData2 = DecimalData2(0),
    val comment: String = "",
    val movementId: Int = 0
) : IMultiLineTableUi {
    val sum: DecimalData2
        get() = DecimalData2(
            count.value * price.value / 1000
        )
}
