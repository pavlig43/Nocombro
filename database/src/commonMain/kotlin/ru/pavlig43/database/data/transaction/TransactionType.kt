package ru.pavlig43.database.data.transaction

import ru.pavlig43.core.data.ItemType

enum class TransactionType(override val displayName: String): ItemType {
    TRANSFER("Перемещение"),
    WRITE_OFF("Списание"),
    INVENTORY("Инвентаризация")
}