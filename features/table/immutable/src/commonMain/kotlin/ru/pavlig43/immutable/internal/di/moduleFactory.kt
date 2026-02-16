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


/**
 * Создаёт Koin модули для неизменяемых таблиц.
 *
 * Регистрирует [NocombroDatabase] и создаёт репозитории для каждого типа таблицы.
 *
 * @param dependencies Зависимости для неизменяемых таблиц
 * @return Список Koin модулей
 */
internal fun moduleFactory(dependencies: ImmutableTableDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        ImmutableTableRepositoryType.entries.forEach { type ->
            single<ImmutableListRepository<*>>(type.qualifier) {
                createImmutableRepository(
                    get(),
                    type
                )
            }
        }
    }
)

/**
 * Типы таблиц для которых создаётся репозиторий.
 *
 * Каждый тип соответствует сущности в базе данных.
 */
internal enum class ImmutableTableRepositoryType {
    /** Документы */
    DOCUMENT,

    /** Декларации */
    DECLARATION,

    /** Связи продуктов и деклараций */
    PRODUCT_DECLARATION,

    /** Продукты */
    PRODUCT,

    /** Поставщики */
    VENDOR,

    /** Транзакции */
    TRANSACTION
}

/**
 * Создаёт репозиторий для указанного типа таблицы.
 *
 * @param db База данных
 * @param type Тип таблицы
 * @return Репозиторий для указанного типа
 */
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

/**
 * Репозиторий для работы с документами.
 */
private class DocumentRepository(db: NocombroDatabase) : ImmutableListRepository<Document> {
    private val dao = db.documentDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteDocumentsByIds(ids) }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun observeOnItems(parentId: Int): Flow<Result<List<Document>>> {
        return dao.observeOnDocuments().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

/**
 * Репозиторий для работы с декларациями.
 */
private class DeclarationRepository(db: NocombroDatabase) : ImmutableListRepository<Declaration> {
    private val dao = db.declarationDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteDeclarationsByIds(ids) }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun observeOnItems(parentId: Int): Flow<Result<List<Declaration>>> {
        return dao.observeOnItems().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

/**
 * Репозиторий для работы с продуктами.
 */
private class ProductRepository(db: NocombroDatabase) : ImmutableListRepository<Product> {
    private val dao = db.productDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteProductsByIds(ids) }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun observeOnItems(parentId: Int): Flow<Result<List<Product>>> {
        return dao.observeOnProducts().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

/**
 * Репозиторий для работы с поставщиками.
 */
private class VendorRepository(db: NocombroDatabase) : ImmutableListRepository<Vendor> {
    private val dao = db.vendorDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteVendorsByIds(ids) }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun observeOnItems(parentId: Int): Flow<Result<List<Vendor>>> {
        return dao.observeOnVendors().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

/**
 * Репозиторий для работы с транзакциями.
 */
private class TransactionRepository(db: NocombroDatabase) : ImmutableListRepository<Transact> {
    private val dao = db.transactionDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return runCatching { dao.deleteTransactionsByIds(ids) }
    }

    @Suppress("UNUSED_PARAMETER")
    override fun observeOnItems(parentId: Int): Flow<Result<List<Transact>>> {
        return dao.observeOnProductTransactions().map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}

/**
 * Репозиторий для работы со связями продуктов и деклараций.
 *
 * **Важно:** Удаление не поддерживается (возвращает success).
 */
private class ProductDeclarationRepository(db: NocombroDatabase) :
    ImmutableListRepository<ProductDeclarationOut> {
    private val dao = db.productDeclarationDao
    override suspend fun deleteByIds(ids: Set<Int>): Result<Unit> {
        return Result.success(Unit)
    }

    override fun observeOnItems(parentId: Int): Flow<Result<List<ProductDeclarationOut>>> {
        require(parentId != 0) {
            "ProductDeclarationRepository requires non-zero parentId for filtering declarations by product"
        }
        return dao.observeOnProductDeclarationOut(parentId).map { Result.success(it) }
            .catch { emit(Result.failure(it)) }
    }
}
