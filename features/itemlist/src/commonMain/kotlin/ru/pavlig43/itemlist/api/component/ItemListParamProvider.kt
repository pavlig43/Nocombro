package ru.pavlig43.itemlist.api.component

import ru.pavlig43.database.data.document.DocumentType

sealed interface ItemListParamProvider
data class DocumentListParamProvider(
    val fullListDocumentTypes: List<DocumentType>,
    val withCheckbox: Boolean,
): ItemListParamProvider

data class DeclarationListParamProvider(
    val withCheckbox: Boolean,
): ItemListParamProvider