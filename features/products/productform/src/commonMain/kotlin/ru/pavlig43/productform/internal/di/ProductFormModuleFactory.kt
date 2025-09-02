package ru.pavlig43.productform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.database.data.product.dao.ProductDeclarationDao
import ru.pavlig43.database.data.product.dao.ProductFilesDao
import ru.pavlig43.itemlist.api.data.ItemListRepository
import ru.pavlig43.productform.api.IProductFormDependencies

private fun baseModule(dependencies: IProductFormDependencies) = module {
    single<DataBaseTransaction> { dependencies.transaction }
    single<ProductDao> { dependencies.productDao }
    single<ProductFilesDao> { dependencies.productFilesDao }
    single<ProductDeclarationDao> { dependencies.productDeclarationDao }
    single<ItemListRepository<Document, DocumentType>> {dependencies.documentListRepository  }

}
internal fun createProductFormModule(dependencies: IProductFormDependencies) = listOf(
    baseModule(dependencies),
    productFormModule
)


