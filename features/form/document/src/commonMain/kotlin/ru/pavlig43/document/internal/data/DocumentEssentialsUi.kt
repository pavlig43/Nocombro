package ru.pavlig43.document.internal.data

import ru.pavlig43.core.data.ItemEssentialsUi
import ru.pavlig43.core.getUTCNow
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import kotlin.time.ExperimentalTime

internal data class DocumentEssentialsUi(
    val displayName: String = "",

    val type: DocumentType? = null,

    val createdAt: Long? = null,

    val comment:String ="",

    override val id: Int = 0,
): ItemEssentialsUi
@OptIn(ExperimentalTime::class)
internal fun DocumentEssentialsUi.toDto(): Document {
    return Document(
        displayName = displayName,
        type = type ?: throw IllegalArgumentException("Document type required"),
        createdAt = createdAt ?: getUTCNow(),
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