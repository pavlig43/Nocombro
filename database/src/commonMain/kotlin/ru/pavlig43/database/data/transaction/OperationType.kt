package ru.pavlig43.database.data.transaction

import ru.pavlig43.core.data.ItemType

enum class OperationType(override val displayName: String): ItemType {
    INCOMING("Приход"),
    OUTGOING("Расход")
}