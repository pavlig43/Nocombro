package ru.pavlig43.itemlist.api.component

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.itemlist.api.model.IItemUi


///////////////////
sealed interface BuilderData<I: IItemUi>{
    val tabTitle: String
    val withCheckbox: Boolean
}
data class DocumentBuilder(
    val fullListDocumentTypes: List<DocumentType>,
    override val withCheckbox: Boolean,
    ):
    BuilderData<ru.pavlig43.itemlist.internal.component.items.document.DocumentItemUi> {
    override val tabTitle: String = "Документы"
}

data class DeclarationBuilder(
    override val withCheckbox: Boolean,
): BuilderData<ru.pavlig43.itemlist.internal.component.items.declaration.DeclarationItemUi> {
    override val tabTitle: String = "Декларации"
}

data class ProductBuilder(
    val fullListProductTypes: List<ProductType>,
    override val withCheckbox: Boolean,

    ):
    BuilderData<ru.pavlig43.itemlist.internal.component.items.product.ProductItemUi> {
    override val tabTitle: String = "Продукты"
}

data class VendorBuilder(
    override val withCheckbox: Boolean,
): BuilderData<ru.pavlig43.itemlist.internal.component.items.vendor.VendorItemUi> {
    override val tabTitle: String ="Поставщики"
}
data class TransactionBuilder(
    val fullListTransactionTypes: List<TransactionType>,
    override val withCheckbox: Boolean,
): BuilderData<ru.pavlig43.itemlist.internal.component.items.transaction.TransactionItemUi> {
    override val tabTitle: String = "Операции"
}