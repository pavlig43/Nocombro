package ru.pavlig43.database.data.transact.pf

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.core.model.SingleItem

/**
 * DTO для отображения полуфабриката в ОПЗС транзакции.
 * Содержит denormalized данные о BatchMovement, Batch, Product и Declaration.
 */
data class PfBD(
    val transactionId: Int = 0,
    val batchId: Int = 0,
    val movementId: Int = 0,
    val count: Int = 0,
    val productId: Int = 0,
    val productName: String = "",
    val declarationId: Int = 0,
    val declarationName: String = "",
    val vendorName: String = "",
    val dateBorn: LocalDate = getCurrentLocalDate(),
    override val id: Int = 0
) : SingleItem
