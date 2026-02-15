package ru.pavlig43.immutable.internal.di

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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
    ImmutableTableRepositoryType.DOCUMENT -> DocumentRepository(db)
    ImmutableTableRepositoryType.DECLARATION -> DeclarationRepository(db)
    ImmutableTableRepositoryType.PRODUCT -> ProductRepository(db)
    ImmutableTableRepositoryType.VENDOR -> VendorRepository(db)
    ImmutableTableRepositoryType.TRANSACTION -> TransactionRepository(db)
    ImmutableTableRepositoryType.PRODUCT_DECLARATION -> ProductDeclarationRepository(db)
}

private class DocumentRepository(db: NocombroDatabase): ImmutableListRepository<Document> {
    private val dao = db.documentDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteDocumentsByIds(ids) }
    }

    override fun observeOnItems(): Flow<Result<List<Document>>> {
        return dao.observeOnDocuments().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

private class DeclarationRepository(db: NocombroDatabase): ImmutableListRepository<Declaration> {
    private val dao = db.declarationDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteDeclarationsByIds(ids) }
    }

    override fun observeOnItems(): Flow<Result<List<Declaration>>> {
        return dao.observeOnItems().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

private class ProductRepository(db: NocombroDatabase): ImmutableListRepository<Product> {
    private val dao = db.productDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteProductsByIds(ids) }
    }

    override fun observeOnItems(): Flow<Result<List<Product>>> {
        return dao.observeOnProducts().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

private class VendorRepository(db: NocombroDatabase): ImmutableListRepository<Vendor> {
    private val dao = db.vendorDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteVendorsByIds(ids) }
    }

    override fun observeOnItems(): Flow<Result<List<Vendor>>> {
        return dao.observeOnVendors().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

private class TransactionRepository(db: NocombroDatabase): ImmutableListRepository<Transact> {
    private val dao = db.transactionDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteTransactionsByIds(ids) }
    }

    override fun observeOnItems(): Flow<Result<List<Transact>>> {
        return dao.observeOnProductTransactions().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

private class ProductDeclarationRepository(db: NocombroDatabase): ImmutableListRepository<ProductDeclarationOut> {
    private val dao = db.productDeclarationDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeOnItems(): Flow<Result<List<ProductDeclarationOut>>> {
        return dao.observeOnProductDeclarationOut().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}
