package ru.pavlig43.database.data.document

import nocombro.database.generated.resources.Res
import nocombro.database.generated.resources.declaration
import org.jetbrains.compose.resources.StringResource
import ru.pavlig43.database.data.common.data.ItemType


enum class DocumentType(override val displayName: String):ItemType {
    Declaration("Декларация"),
    GOST("ГОСТ")
}