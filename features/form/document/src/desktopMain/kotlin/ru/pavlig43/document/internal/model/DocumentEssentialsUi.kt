package ru.pavlig43.document.internal.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.getCurrentLocalDate
import ru.pavlig43.mutable.api.singleLine.model.ISingleLineTableUi
import kotlin.time.ExperimentalTime

internal data class DocumentEssentialsUi(
    val displayName: String = "",

    val type: DocumentType? = null,

    val createdAt: LocalDate = getCurrentLocalDate(),

    val comment:String ="",

    val id: Int = 0,

    val syncId: String = defaultSyncId(),

    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    val deletedAt: LocalDateTime? = null,
): ISingleLineTableUi
@OptIn(ExperimentalTime::class)
internal fun DocumentEssentialsUi.toDto(): Document {
    return Document(
        displayName = displayName,
        type = type ?: throw IllegalArgumentException("Document type required"),
        createdAt = createdAt,
        comment = comment,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
internal fun Document.toUi(): DocumentEssentialsUi {
    return DocumentEssentialsUi(
        displayName = displayName,
        type = type ,
        createdAt = createdAt ,
        comment = comment,
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
