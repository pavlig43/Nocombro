package ru.pavlig43.document.internal.data

import kotlinx.datetime.LocalDate
import ru.pavlig43.core.getCurrentLocalDate
import ru.pavlig43.core.model.ItemEssentialsUi
import ru.pavlig43.tablecore.model.IMultiLineTableUi
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.tablecore.model.ISingleLineTableUi
import kotlin.time.ExperimentalTime

internal data class DocumentEssentialsUi(
    val displayName: String = "",

    val type: DocumentType? = null,

    val createdAt: LocalDate = getCurrentLocalDate(),

    val comment:String ="",

    override val id: Int = 0,
): ItemEssentialsUi, ISingleLineTableUi
@OptIn(ExperimentalTime::class)
internal fun DocumentEssentialsUi.toDto(): Document {
    return Document(
        displayName = displayName,
        type = type ?: throw IllegalArgumentException("Document type required"),
        createdAt = createdAt,
        comment = comment,
        id = id
    )
}
internal fun Document.toUi(): DocumentEssentialsUi {
    return DocumentEssentialsUi(
        displayName = displayName,
        type = type ,
        createdAt = createdAt ,
        comment = comment,
        id = id
    )
}