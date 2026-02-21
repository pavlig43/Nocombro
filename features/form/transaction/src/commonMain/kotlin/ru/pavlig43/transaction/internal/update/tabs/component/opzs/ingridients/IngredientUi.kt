package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingridients

import ru.pavlig43.tablecore.model.IMultiLineTableUi

data class IngredientUi(
    override val composeId: Int,
    val id: Int,
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val movementId: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val vendorName: String = "",
    val count: Int = 0,
) : IMultiLineTableUi
