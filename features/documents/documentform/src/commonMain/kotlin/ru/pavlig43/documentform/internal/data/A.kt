package ru.pavlig43.documentform.internal.data

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.manageitem.internal.data.ItemEssentialsUi

internal data class DocumentEssentialsUi(
    val displayName: String = "",

    val type: DocumentType? = null,

    val createdAt: Long? = null,

    val comment:String ="",

    override val id: Int = 0,
): ItemEssentialsUi