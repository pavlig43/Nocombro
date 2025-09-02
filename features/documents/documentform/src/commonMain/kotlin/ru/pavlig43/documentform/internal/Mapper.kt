package ru.pavlig43.documentform.internal

import ru.pavlig43.core.UTC
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.manageitem.api.data.RequireValues
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun RequireValues.toDocument(): Document {
    return Document(
        displayName = name,
        type = type as DocumentType,
        createdAt = createdAt?: UTC(Clock.System.now().toEpochMilliseconds()),
        comment = comment,
        id = id
    )
}