package ru.pavlig43.transaction.internal.update.tabs.component.buy

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData
import ru.pavlig43.core.model.DecimalFormat
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.tablecore.model.IMultiLineTableUi

@Suppress("MagicNumber")
data class BuyUi(
    override val composeId: Int,
    val id: Int,
    val productId: Int = 0,
    val productName: String = "",
    val count: DecimalData = DecimalData(0, DecimalFormat.Decimal3()),
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val dateBorn: LocalDate = getCurrentLocalDate(),
    val price: DecimalData = DecimalData(0, DecimalFormat.Decimal2()),
    val comment: String = "",
    val batchId: Int = 0,
    val movementId: Int = 0
) : IMultiLineTableUi {
    val sum: DecimalData
        get() = DecimalData(
            (count.value.toLong() * price.value / 1000).toInt(),
            DecimalFormat.Decimal2()
        )
}