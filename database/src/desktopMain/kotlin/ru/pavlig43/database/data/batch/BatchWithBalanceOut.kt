package ru.pavlig43.database.data.batch

import kotlinx.datetime.LocalDate

/**
 * DTO партии с вычисленным остатком из движений (BatchMovement).
 *
 * Остаток (balance) вычисляется как сумма INCOMING - сумма OUTGOING.
 *

 */
data class BatchWithBalanceOut(
    val batchId: Int,
    val productName: String,
    val balance: Int,
    val vendorName: String,
    val dateBorn: LocalDate
)
