package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.emptyDate
import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class IngredientUi(
    override val composeId: Int,
    val id: Int,
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val dateBorn: LocalDate = emptyDate,
    val movementId: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val vendorName: String = "",
    val balance: Int = 0,
) : IMultiLineTableUi
