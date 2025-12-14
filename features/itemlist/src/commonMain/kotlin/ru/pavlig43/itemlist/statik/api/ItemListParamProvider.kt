package ru.pavlig43.itemlist.statik.api

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType

sealed interface ItemListParamProvider{
    val tabTitle: String
}
data class DocumentListParamProvider(
    val fullListDocumentTypes: List<DocumentType>,
    val withCheckbox: Boolean,

): ItemListParamProvider{
    override val tabTitle: String = "Документы"
}

data class DeclarationListParamProvider(
    val withCheckbox: Boolean,
): ItemListParamProvider{
    override val tabTitle: String = "Декларации"
}

data class ProductListParamProvider(
    val fullListProductTypes: List<ProductType>,
    val withCheckbox: Boolean,

): ItemListParamProvider{
    override val tabTitle: String = "Продукты"
}

data class VendorListParamProvider(
    val withCheckbox: Boolean,
): ItemListParamProvider{
    override val tabTitle: String ="Поставщики"
}
data class TransactionListParamProvider(
    val fullListTransactionTypes: List<TransactionType>,
    val withCheckbox: Boolean,
): ItemListParamProvider{
    override val tabTitle: String = "Операции"
}