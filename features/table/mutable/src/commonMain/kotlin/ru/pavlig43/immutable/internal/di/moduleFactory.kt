package ru.pavlig43.immutable.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.immutable.api.MutableTableDependencies
import ru.pavlig43.immutable.api.dependencies
import ru.pavlig43.immutable.internal.data.MutableListRepository


internal fun moduleFactory(dependencies: MutableTableDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        ImmutableTableRepositoryType.entries.forEach {type ->
            single<MutableListRepository<*>>(type.qualifier) { createImmutableRepository(get(), type) }
        }
    }
)
internal enum class ImmutableTableRepositoryType{
    DOCUMENT,
    DECLARATION,
    PRODUCT,
    VENDOR,
    TRANSACTION
}
private fun createImmutableRepository(
    db: NocombroDatabase,
    type: ImmutableTableRepositoryType
): MutableListRepository<*> = when (type) {
    ImmutableTableRepositoryType.DOCUMENT -> createDocumentRepository(db)
    ImmutableTableRepositoryType.DECLARATION -> createDeclarationRepository(db)
    ImmutableTableRepositoryType.PRODUCT -> createProductRepository(db)
    ImmutableTableRepositoryType.VENDOR -> createVendorRepository(db)
    ImmutableTableRepositoryType.TRANSACTION -> createTransactionRepository(db)
}

private fun createDocumentRepository(db: NocombroDatabase): MutableListRepository<Document> {
    val dao = db.documentDao
    return MutableListRepository(
        delete = dao::deleteDocumentsByIds,
        observe = dao::observeOnDocuments,
    )
}
private fun createDeclarationRepository(db: NocombroDatabase): MutableListRepository<Declaration> {
    val dao = db.declarationDao
    return MutableListRepository(
        delete = dao::deleteDeclarationsByIds,
        observe = dao::observeOnItems,
    )
}

private fun createProductRepository(db: NocombroDatabase): MutableListRepository<Product> {
    val dao = db.productDao
    return MutableListRepository(
        delete = dao::deleteProductsByIds,
        observe = dao::observeOnProducts,
    )
}

private fun createVendorRepository(db: NocombroDatabase): MutableListRepository<Vendor> {
    val dao = db.vendorDao
    return MutableListRepository(
        delete = dao::deleteVendorsByIds,
        observe = dao::observeOnVendors,
    )
}

private fun createTransactionRepository(db: NocombroDatabase): MutableListRepository<Transaction> {
    val dao = db.productTransactionDao
    return MutableListRepository(
        delete = dao::deleteTransactionsByIds,
        observe = dao::observeOnProductTransactions,
    )
}
