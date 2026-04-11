package ru.pavlig43.database.data.files

import kotlinx.serialization.Serializable

@Serializable
enum class OwnerType {
    DECLARATION,
    PRODUCT,
    VENDOR,
    DOCUMENT,
    TRANSACTION,
    EXPENSE
}
