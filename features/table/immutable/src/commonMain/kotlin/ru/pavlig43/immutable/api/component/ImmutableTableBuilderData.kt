package ru.pavlig43.immutable.api.component

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.immutable.internal.component.items.productDeclaration.ProductDeclarationTableUi
import ru.pavlig43.tablecore.model.ITableUi


///////////////////
sealed interface ImmutableTableBuilderData<I: ITableUi>{
    val tabTitle: String
    val withCheckbox: Boolean

}
data class DocumentImmutableTableBuilder(
    val fullListDocumentTypes: List<DocumentType>,
    override val withCheckbox: Boolean,
    ):
    ImmutableTableBuilderData<ru.pavlig43.immutable.internal.component.items.document.DocumentTableUi> {
    override val tabTitle: String = "Документы"
}

data class DeclarationImmutableTableBuilder(
    override val withCheckbox: Boolean,
): ImmutableTableBuilderData<ru.pavlig43.immutable.internal.component.items.declaration.DeclarationTableUi> {
    override val tabTitle: String = "Декларации"
}
data class ProductDeclarationImmutableTableBuilder(
    val productId: Int
): ImmutableTableBuilderData<ProductDeclarationTableUi> {
    override val tabTitle: String = "Декларации"
    override val withCheckbox: Boolean = false

}

data class ProductImmutableTableBuilder(
    val fullListProductTypes: List<ProductType>,
    override val withCheckbox: Boolean,

    ):
    ImmutableTableBuilderData<ru.pavlig43.immutable.internal.component.items.product.ProductTableUi> {
    override val tabTitle: String = "Продукты"
}

data class VendorImmutableTableBuilder(
    override val withCheckbox: Boolean,
): ImmutableTableBuilderData<ru.pavlig43.immutable.internal.component.items.vendor.VendorTableUi> {
    override val tabTitle: String ="Поставщики"
}
data class TransactionImmutableTableBuilder(
    val fullListTransactionTypes: List<TransactionType>,
    override val withCheckbox: Boolean,
): ImmutableTableBuilderData<ru.pavlig43.immutable.internal.component.items.transaction.TransactionTableUi> {
    override val tabTitle: String = "Транзакции"
}