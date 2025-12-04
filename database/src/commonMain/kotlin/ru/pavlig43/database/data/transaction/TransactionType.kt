package ru.pavlig43.database.data.transaction

enum class TransactionType(val displayName: String) {
    TRANSFER("Перемещение"),
    WRITE_OFF("Списание"),
    INVENTORY("Инвентаризация")
}