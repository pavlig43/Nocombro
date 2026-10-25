package ru.pavlig43.document.internal.model

import androidx.compose.foundation.text.input.TextFieldState
import ru.pavlig43.mutable.api.singleLine.component.SingleLineInputState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.sync.defaultSyncId
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.datetime.getCurrentLocalDate
import kotlin.time.ExperimentalTime

internal data class DocumentEssentialsUi(
    val displayNameState: TextFieldState = TextFieldState(),

    val type: DocumentType? = null,

    val createdAt: LocalDate = getCurrentLocalDate(),

    val commentState: TextFieldState = TextFieldState(),

    val id: Int = 0,

    val syncId: String = defaultSyncId(),

    val updatedAt: LocalDateTime = defaultUpdatedAt(),

    val deletedAt: LocalDateTime? = null,
) : SingleLineInputState {
    val displayName: String get() = displayNameState.text.toString()
    val comment: String get() = commentState.text.toString()
    override val textFields get() = listOf(displayNameState, commentState)
}
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
        displayNameState = TextFieldState(displayName),
        type = type ,
        createdAt = createdAt ,
        commentState = TextFieldState(comment),
        id = id,
        syncId = syncId,
        updatedAt = updatedAt,
        deletedAt = deletedAt,
    )
}
