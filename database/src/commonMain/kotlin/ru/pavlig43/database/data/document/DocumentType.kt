package ru.pavlig43.database.data.document

import ru.pavlig43.database.data.common.data.ItemType


enum class DocumentType(override val displayName: String):ItemType {
    Declaration("Декларация"),
    GOST("ГОСТ"),
    SPECIFICATION("Спецификация")
}