package ru.pavlig43.database.data.batch

import ru.pavlig43.core.model.ItemType

enum class StorageLocation(override val displayName: String) : ItemType {
    MAIN("Основной"),
    EXPERIMENTAL("Для экспериментов"),
}
