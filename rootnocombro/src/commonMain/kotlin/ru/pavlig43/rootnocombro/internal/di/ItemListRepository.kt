package ru.pavlig43.rootnocombro.internal.di

import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.itemlist.api.data.ItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType

internal val itemListRepositoryModule = module {
    factory<ItemListRepository<Document,DocumentType>>(named(ItemListType.Document.name)) { getDocumentListRepository(get()) }
    factory<ItemListRepository<Product, ProductType>>(named(ItemListType.Product.name)) { getProductListRepository(get()) }
}



private fun getDocumentListRepository(
    db: NocombroDatabase,
): ItemListRepository<Document, DocumentType> {
    val documentDao = db.documentDao
    return ItemListRepository<Document, DocumentType>(
        tableName = DOCUMENT_TABLE_NAME,
        deleteByIds = documentDao::deleteDocumentsByIds,
        observeOnItems = documentDao::observeOnItems
    )
}

private fun getProductListRepository(
    db: NocombroDatabase
): ItemListRepository<Product, ProductType> {
    val productDao = db.productDao
    return ItemListRepository<Product, ProductType>(
        tableName = PRODUCT_TABLE_NAME,
        deleteByIds = productDao::deleteProductsByIds,
        observeOnItems = productDao::observeOnItems
    )
}