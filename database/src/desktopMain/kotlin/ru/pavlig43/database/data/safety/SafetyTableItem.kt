package ru.pavlig43.database.data.safety


data class SafetyTableItem(
    val productId: Int,
    val productName: String,
    val vendorName: String,
    val count: Long,
    val reorderPoint: Long,
    val orderQuantity: Long,
)
