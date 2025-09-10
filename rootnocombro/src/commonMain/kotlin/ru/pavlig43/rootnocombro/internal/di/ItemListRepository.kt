package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
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
    db: NocombroDatabase,
): ItemListRepository<Document, DocumentType> {
    val documentDao = db.documentDao
    return ItemListRepository<Document, DocumentType>(
        tag = "DocumentRepository",
        deleteByIds = documentDao::deleteDocumentsByIds,
        observeAllItem = documentDao::observeAllDocument,
        observeItemsByTypes = documentDao::observeDocumentsByTypes
    )
}

private fun getProductListRepository(
    db: NocombroDatabase
): ItemListRepository<Product, ProductType> {
    val productDao = db.productDao
    return ItemListRepository<Product, ProductType>(
        tag = "Product list Repository",
        deleteByIds = productDao::deleteProductsByIds,
        observeAllItem = productDao::observeAllProduct,
        observeItemsByTypes = productDao::observeProductsByTypes
    )
}