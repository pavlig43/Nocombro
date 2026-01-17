package ru.pavlig43.database.data.transaction

import ru.pavlig43.core.model.ItemType

enum class TransactionType(override val displayName: String): ItemType {
    BUY("Покупка"),
    SALE("Продажа"),
    OPZS("ОПЗС"),
    WRITE_OFF("Списание"),
    INVENTORY("Инвентаризация")
}