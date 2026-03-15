package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.DecimalData3
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.datetime.emptyDate
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class IngredientUi(
    override val composeId: Int,
    val id: Int,
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val dateBorn: LocalDate = emptyDate,
    val movementId: Int = 0,
    val productId: Int = 0,
    val productType: ProductType,
    val productName: String = "",
    val vendorName: String = "",
    val balance: DecimalData3 = DecimalData3(0),
) : IMultiLineTableUi
