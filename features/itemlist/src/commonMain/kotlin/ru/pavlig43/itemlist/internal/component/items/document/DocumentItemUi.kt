package ru.pavlig43.itemlist.internal.component.items.document

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.model.IItemUi

data class DocumentItemUi(
    override val id: Int,
    val displayName: String,
    val type: DocumentType,
    val createdAt: LocalDate,
    val comment: String = "",
) : IItemUi