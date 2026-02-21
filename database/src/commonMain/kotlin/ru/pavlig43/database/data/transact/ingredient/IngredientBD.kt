package ru.pavlig43.database.data.transact.ingredient

import ru.pavlig43.core.model.CollectionObject

/**
 * DTO для отображения ингредиента в OPZS транзакции.
 * Содержит denormalized данные о BatchMovement, Batch, Product и Vendor.
 */
data class IngredientBD(
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val movementId: Int = 0,
    val count: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val vendorName: String = "",
    override val id: Int = 0
) : CollectionObject
