package ru.pavlig43.database.data.transact.ingredient

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.model.CollectionObject
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.datetime.emptyDate

/**
 * DTO для отображения ингредиента в OPZS транзакции.
 * Содержит denormalized данные о BatchMovement, Batch, Product и Vendor.
 */
data class IngredientBD(
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val dateBorn: LocalDate = emptyDate,
    val movementId: Int = 0,
    val count: Long = 0,
    val productType: ProductType,
    val productId: Int = 0,
    val productName: String = "",
    val vendorName: String = "",
    val syncId: String = defaultSyncId(),
    override val id: Int = 0,
) : CollectionObject
