package ru.pavlig43.immutable.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.database.data.transact.Transact
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.immutable.internal.data.ImmutableListRepository


internal fun moduleFactory(dependencies: ImmutableTableDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        ImmutableTableRepositoryType.entries.forEach {type ->
            single<ImmutableListRepository<*>>(type.qualifier) { createImmutableRepository(get(), type) }
        }
    }
)
internal enum class ImmutableTableRepositoryType{
    DOCUMENT,
    DECLARATION,
    PRODUCT_DECLARATION,
    PRODUCT,
    VENDOR,
    TRANSACTION
}
private fun createImmutableRepository(
    db: NocombroDatabase,
    type: ImmutableTableRepositoryType
): ImmutableListRepository<*> = when (type) {
    ImmutableTableRepositoryType.DOCUMENT -> createDocumentRepository(db)
    ImmutableTableRepositoryType.DECLARATION -> createDeclarationRepository(db)
    ImmutableTableRepositoryType.PRODUCT -> createProductRepository(db)
    ImmutableTableRepositoryType.VENDOR -> createVendorRepository(db)
    ImmutableTableRepositoryType.TRANSACTION -> createTransactionRepository(db)
    ImmutableTableRepositoryType.PRODUCT_DECLARATION -> createProductDeclarationRepository(db)
}

private fun createDocumentRepository(db: NocombroDatabase): ImmutableListRepository<Document> {
    val dao = db.documentDao
    return ImmutableListRepository(
        delete = dao::deleteDocumentsByIds,
        observe = dao::observeOnDocuments,
    )
}
private fun createDeclarationRepository(db: NocombroDatabase): ImmutableListRepository<Declaration> {
    val dao = db.declarationDao
    return ImmutableListRepository(
        delete = dao::deleteDeclarationsByIds,
        observe = dao::observeOnItems,
    )
}

private fun createProductRepository(db: NocombroDatabase): ImmutableListRepository<Product> {
    val dao = db.productDao
    return ImmutableListRepository(
        delete = dao::deleteProductsByIds,
        observe = dao::observeOnProducts,
    )
}

private fun createVendorRepository(db: NocombroDatabase): ImmutableListRepository<Vendor> {
    val dao = db.vendorDao
    return ImmutableListRepository(
        delete = dao::deleteVendorsByIds,
        observe = dao::observeOnVendors,
    )
}

private fun createTransactionRepository(db: NocombroDatabase): ImmutableListRepository<Transact> {
    val dao = db.transactionDao
    return ImmutableListRepository(
        delete = dao::deleteTransactionsByIds,
        observe = dao::observeOnProductTransactions,
    )
}
private fun createProductDeclarationRepository(db: NocombroDatabase): ImmutableListRepository<ProductDeclarationOut>{
    val dao = db.productDeclarationDao
    return ImmutableListRepository(
        delete = {},
        observe = dao::observeOnProductDeclarationOut
    )
}
