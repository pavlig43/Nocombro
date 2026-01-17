package ru.pavlig43.database.data.document

import ru.pavlig43.core.model.ItemType


enum class DocumentType(override val displayName: String): ItemType {
    GOST("ГОСТ"),
    SPECIFICATION("Спецификация");

}