package ru.pavlig43.itemlist.api.component

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.itemlist.api.model.ITableUi


///////////////////
sealed interface BuilderData<I: ITableUi>{
    val tabTitle: String
    val withCheckbox: Boolean
}
data class DocumentBuilder(
    val fullListDocumentTypes: List<DocumentType>,
    override val withCheckbox: Boolean,
    ):
    BuilderData<ru.pavlig43.itemlist.internal.component.items.document.DocumentTableUi> {
    override val tabTitle: String = "Документы"
}

data class DeclarationBuilder(
    override val withCheckbox: Boolean,
): BuilderData<ru.pavlig43.itemlist.internal.component.items.declaration.DeclarationTableUi> {
    override val tabTitle: String = "Декларации"
}

data class ProductBuilder(
    val fullListProductTypes: List<ProductType>,
    override val withCheckbox: Boolean,

    ):
    BuilderData<ru.pavlig43.itemlist.internal.component.items.product.ProductTableUi> {
    override val tabTitle: String = "Продукты"
}

data class VendorBuilder(
    override val withCheckbox: Boolean,
): BuilderData<ru.pavlig43.itemlist.internal.component.items.vendor.VendorTableUi> {
    override val tabTitle: String ="Поставщики"
}
data class TransactionBuilder(
    val fullListTransactionTypes: List<TransactionType>,
    override val withCheckbox: Boolean,
): BuilderData<ru.pavlig43.itemlist.internal.component.items.transaction.TransactionTableUi> {
    override val tabTitle: String = "Транзакции"
}