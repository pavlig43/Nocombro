package ru.pavlig43.productform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType
import ru.pavlig43.productform.api.IProductFormDependencies

private fun baseModule(dependencies: IProductFormDependencies) = module {
    single<NocombroDatabase> { dependencies.db }
    single<DataBaseTransaction> { dependencies.transaction }
    single<IItemListRepository<Document, DocumentType>>(named(ItemListType.Document.name)) {dependencies.documentListRepository  }
    single<IItemListRepository<Product, ProductType>>(named(ItemListType.Product.name)) {dependencies.productListRepository  }

}
internal fun createProductFormModule(dependencies: IProductFormDependencies) = listOf(
    baseModule(dependencies),
    productFormModule
)


