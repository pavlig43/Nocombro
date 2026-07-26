package ru.pavlig43.database.data.transact

import ru.pavlig43.core.model.ItemType

enum class StockOperationReason(override val displayName: String) : ItemType {
    EXPERIMENT("Эксперимент"),
    EXPIRED("Истёк срок"),
    SAMPLE("Пробник"),
    DAMAGED("Порча"),
    RETURN("Возврат"),
    OTHER("Прочее"),
}
