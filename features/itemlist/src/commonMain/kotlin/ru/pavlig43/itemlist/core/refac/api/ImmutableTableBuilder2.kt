package ru.pavlig43.itemlist.core.refac.api

import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.transaction.TransactionType
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ru.pavlig43.itemlist.statik.internal.component.DeclarationItemUi
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ru.pavlig43.itemlist.statik.internal.component.ProductItemUi
import ru.pavlig43.itemlist.statik.internal.component.TransactionItemUi
import ru.pavlig43.itemlist.statik.internal.component.VendorItemUi

sealed interface ImmutableTableBuilder2{
    val tabTitle: String
    val withCheckbox: Boolean
}
data class DocumentListParamProvider(
    val fullListDocumentTypes: List<DocumentType>,
    override val withCheckbox: Boolean,

    ): ImmutableTableBuilder2{
    override val tabTitle: String = "Документы"
}

data class DeclarationListParamProvider(
    override val withCheckbox: Boolean,
): ImmutableTableBuilder2{
    override val tabTitle: String = "Декларации"
}

data class ProductListParamProvider(
    val fullListProductTypes: List<ProductType>,
    override val withCheckbox: Boolean,

    ): ImmutableTableBuilder2{
    override val tabTitle: String = "Продукты"
}

data class VendorListParamProvider(
    override val withCheckbox: Boolean,
): ImmutableTableBuilder2{
    override val tabTitle: String ="Поставщики"
}
data class TransactionListParamProvider(
    val fullListTransactionTypes: List<TransactionType>,
    override val withCheckbox: Boolean,
): ImmutableTableBuilder2{
    override val tabTitle: String = "Операции"
}
///////////////////
sealed interface BuilderData<I: IItemUi>{
    val tabTitle: String
    val withCheckbox: Boolean
}
data class DocumentBuilder(
    val fullListDocumentTypes: List<DocumentType>,
    override val withCheckbox: Boolean,
    ): BuilderData<DocumentItemUi>{
    override val tabTitle: String = "Документы"
}

data class DeclarationBuilder(
    override val withCheckbox: Boolean,
): BuilderData<DeclarationItemUi>{
    override val tabTitle: String = "Декларации"
}

data class ProductBuilder(
    val fullListProductTypes: List<ProductType>,
    override val withCheckbox: Boolean,

    ): BuilderData<ProductItemUi>{
    override val tabTitle: String = "Продукты"
}

data class VendorBuilder(
    override val withCheckbox: Boolean,
): BuilderData<VendorItemUi>{
    override val tabTitle: String ="Поставщики"
}
data class TransactionBuilder(
    val fullListTransactionTypes: List<TransactionType>,
    override val withCheckbox: Boolean,
): BuilderData<TransactionItemUi>{
    override val tabTitle: String = "Операции"
}