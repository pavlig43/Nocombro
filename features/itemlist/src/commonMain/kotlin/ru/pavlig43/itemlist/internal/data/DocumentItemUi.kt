package ru.pavlig43.itemlist.internal.data

import ru.pavlig43.coreui.itemlist.IItemUi
import ru.pavlig43.database.data.document.DocumentType

internal data class DocumentItemUi(
    override val id: Int,
    override val displayName: String,
    val type: DocumentType,
    val createdAt: Long,
    val comment: String = "",
) : IItemUi