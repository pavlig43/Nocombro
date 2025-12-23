package ru.pavlig43.database.data.transaction

import ru.pavlig43.core.data.ItemType

enum class TransactionType(override val displayName: String): ItemType {
    BUY("Покупка"),
    SALE("Продажа"),
    WRITE_OFF("Списание"),
    INVENTORY("Инвентаризация")
}