package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.database.data.product.dao.ProductDao
import ru.pavlig43.itemlist.api.data.ItemListRepository

internal val itemListRepositoryModule = module {
    factory<ItemListRepository<Document,DocumentType>>(named(ItemListType.Document.name)) { getDocumentListRepository(get()) }
    factory<ItemListRepository<Product, ProductType>>(named(ItemListType.Product.name)) { getProductListRepository(get()) }
}

internal enum class ItemListType {
    Document,
    Product
}

private fun getDocumentListRepository(
    documentDao: DocumentDao,
): ItemListRepository<Document, DocumentType> {
    return ItemListRepository<Document, DocumentType>(
        tag = "DocumentRepository",
        deleteByIds = documentDao::deleteDocumentsByIds,
        observeAllItem = documentDao::observeAllDocument,
        observeItemsByTypes = documentDao::observeDocumentsByTypes
    )
}

private fun getProductListRepository(
    productDao: ProductDao
): ItemListRepository<Product, ProductType> {
    return ItemListRepository<Product, ProductType>(
        tag = "Product list Repository",
        deleteByIds = productDao::deleteProductsByIds,
        observeAllItem = productDao::observeAllProduct,
        observeItemsByTypes = productDao::observeProductsByTypes
    )
}