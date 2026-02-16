package ru.pavlig43.immutable.api.component

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transact.TransactionType
import ru.pavlig43.immutable.internal.component.items.productDeclaration.ProductDeclarationTableUi
import ru.pavlig43.tablecore.model.IMultiLineTableUi


///////////////////
sealed interface ImmutableTableBuilderData<I: IMultiLineTableUi>{
    val tabTitle: String
    val withCheckbox: Boolean

    /**
     * Родительский идентификатор для фильтрации связанных данных.
     *
     * Используется для таблиц с отношениями "один-ко-многим", когда нужно отобразить
     * только элементы, связанные с определённой родительской сущностью.
     *
     * Например, для отображения деклараций конкретного продукта:
     * - `parentId = productId` — только декларации этого продукта
     * - `parentId = 0` — все декларации (без фильтрации)
     */
    val parentId: Int
        get() = 0

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
    override val parentId: Int
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