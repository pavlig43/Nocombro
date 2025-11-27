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
import ru.pavlig43.itemlist.api.data.DeclarationListRepository
import ru.pavlig43.itemlist.api.data.DocumentListRepository
import ru.pavlig43.itemlist.api.data.DefaultItemListRepository
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType
import ru.pavlig43.itemlist.api.data.ProductListRepository
import ru.pavlig43.itemlist.api.data.VendorListRepository

internal val IItemListRepositoryModule = module {
    factory<IItemListRepository<Document,DocumentType>>(named(ItemListType.Document.name)) {
        getDocumentListRepository(
            get()
        )
    }
    factory<IItemListRepository<Product, ProductType>>(named(ItemListType.Product.name)) {
        getProductListRepository(
            get()
        )
    }

}




private fun getDocumentListRepository(
    db: NocombroDatabase,
): IItemListRepository<Document, DocumentType> {
    val documentDao = db.documentDao
    return DefaultItemListRepository<Document, DocumentType>(
        tableName = DOCUMENT_TABLE_NAME,
        deleteByIds = documentDao::deleteDocumentsByIds,
        observeOnItems = documentDao::observeOnItems
    )
}

private fun getProductListRepository(
    db: NocombroDatabase
): IItemListRepository<Product, ProductType> {
    val productDao = db.productDao
    return DefaultItemListRepository(
        tableName = PRODUCT_TABLE_NAME,
        deleteByIds = productDao::deleteProductsByIds,
        observeOnItems = productDao::observeOnItems
    )
}

