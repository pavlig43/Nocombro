package ru.pavlig43.itemlist.api

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType

sealed interface ItemListParamProvider
data class DocumentListParamProvider(
    val fullListDocumentTypes: List<DocumentType>,
    val withCheckbox: Boolean,
): ItemListParamProvider

data class DeclarationListParamProvider(
    val withCheckbox: Boolean,
): ItemListParamProvider

data class ProductListParamProvider(
    val fullListProductTypes: List<ProductType>,
    val withCheckbox: Boolean,
): ItemListParamProvider

data class VendorListParamProvider(
    val withCheckbox: Boolean,
): ItemListParamProvider