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
import ru.pavlig43.database.data.vendor.VENDOR_TABLE_NAME
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.database.data.vendor.VendorType
import ru.pavlig43.itemlist.api.data.DeclarationListRepository
import ru.pavlig43.itemlist.api.data.DocumentListRepository
import ru.pavlig43.itemlist.api.data.DefaultItemListRepository
import ru.pavlig43.itemlist.api.data.IItemListRepository
import ru.pavlig43.itemlist.api.data.ItemListType

internal val IItemListRepositoryModule = module {
    factoryOf(::DocumentListRepository)
    factoryOf(::DeclarationListRepository)
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
    factory<IItemListRepository<Vendor, VendorType>>(named(ItemListType.Vendor.name)) {
        getVendorListRepository(
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

private fun getVendorListRepository(
    db: NocombroDatabase
): IItemListRepository<Vendor, VendorType> {
    val dao = db.vendorDao
    return DefaultItemListRepository(
        tableName = VENDOR_TABLE_NAME,
        deleteByIds = dao::deleteVendorsByIds,
        observeOnItems = dao::observeOnItems
    )
}

//private fun getTransactionListRepository(db: NocombroDatabase): IItemListRepository<ProductTransactionIn> {}
