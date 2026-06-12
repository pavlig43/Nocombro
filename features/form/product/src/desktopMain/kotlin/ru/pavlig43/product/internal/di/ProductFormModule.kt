package ru.pavlig43.product.internal.di

import kotlinx.coroutines.flow.Flow
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.files.FileBD
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductSpecification
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.data.sync.mirror.MirrorDeletionJournalRepository
import ru.pavlig43.database.inTransaction
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.TransactionalUpdateCollectionRepository
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.TransactionalCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.TransactionalUpdateSingleLineRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.product.internal.update.tabs.specification.createDefaultProductSpecification
import ru.pavlig43.product.internal.update.tabs.specification.ProductSpecificationCompositionGenerator
import ru.pavlig43.product.internal.update.tabs.specification.ProductSpecificationPdfGenerator
import ru.pavlig43.product.internal.update.tabs.specification.ProductSpecificationPdfRepository

internal fun createProductFormModule(dependencies: ProductFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single { ProductSpecificationPdfGenerator() }
        single { ProductSpecificationCompositionGenerator(get<NocombroDatabase>().compositionDao) }
        single {
            ProductSpecificationPdfRepository(
                fileDao = get<NocombroDatabase>().fileDao,
                remoteFileStorageGateway = get<FilesDependencies>().remoteFileStorageGateway,
                pdfGenerator = get(),
            )
        }
        single<CreateSingleItemRepository<Product>> { ProductCreateRepository(get()) }
        single<UpdateSingleLineRepository<Product>>(SingleRepositoryType.ESSENTIALS.qualifier) {
            ProductUpdateRepository(get())
        }
        single<UpdateSingleLineRepository<ProductSpecification>>(SingleRepositoryType.SPECIFICATION.qualifier) {
            ProductSpecificationUpdateRepository(get())
        }

        single<UpdateCollectionRepository<CompositionOut, CompositionIn>>(
            UpdateCollectionRepositoryType.Composition.qualifier
        ) { CompositionCollectionRepository(get()) }

        singleOf(::ProductDeclarationRepository)

        single<UpdateSingleLineRepository<SafetyStock>>(SingleRepositoryType.SAFETY.qualifier) {
            SafetyStockUpdateRepository(get())
        }
    }
)

private class ProductCreateRepository(
    db: NocombroDatabase,
) : TransactionalCreateSingleItemRepository<Product>(
    inWriteTransaction = { block -> db.inTransaction(block) },
) {
    private val dao = db.productDao

    override suspend fun validate(item: Product): Result<Unit> = dao.isCanSave(item)

    override suspend fun createInDb(item: Product): Int = dao.create(item).toInt()
}

private class ProductUpdateRepository(
    db: NocombroDatabase,
) : TransactionalUpdateSingleLineRepository<Product>(
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.productDao

    override suspend fun getInit(id: Int): Result<Product> {
        return runCatching {
            dao.getProduct(id)
        }
    }

    override fun prepareForUpdate(item: Product): Product = item.copy(updatedAt = defaultUpdatedAt())

    override suspend fun validate(item: Product): Result<Unit> = dao.isCanSave(item)

    override suspend fun updateInDb(item: Product) {
        dao.updateProduct(item)
    }
}

private class CompositionCollectionRepository(
    db: NocombroDatabase,
) : TransactionalUpdateCollectionRepository<CompositionOut, CompositionIn>(
    entitySyncKeyOf = CompositionIn::syncId,
    inWriteTransaction = { block -> db.inTransaction(block) },
    captureHardDeletes = MirrorDeletionJournalRepository(db)::captureHardDeletesInCurrentTransaction,
) {

    private val dao = db.compositionDao

    override suspend fun getInit(id: Int): Result<List<CompositionOut>> {
        return runCatching {
            dao.getCompositionOut(id)
        }
    }

    override fun prepareForUpsert(item: CompositionIn): CompositionIn {
        return item.copy(updatedAt = defaultUpdatedAt())
    }

    override suspend fun deleteByIds(ids: List<Int>) {
        dao.deleteCompositions(ids)
    }

    override suspend fun upsertItems(items: List<CompositionIn>) {
        dao.upsertComposition(items)
    }
}

internal enum class UpdateCollectionRepositoryType {
    Composition,

}
internal enum class SingleRepositoryType{
    ESSENTIALS,
    SPECIFICATION,
    SAFETY
}

internal class ProductSpecificationUpdateRepository(
    private val db: NocombroDatabase,
) : UpdateSingleLineRepository<ProductSpecification> {

    private val dao = db.productSpecificationDao

    override suspend fun getInit(id: Int): Result<ProductSpecification> {
        return runCatching {
            dao.getByProductId(id) ?: createDefaultProductSpecification(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<ProductSpecification>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            db.inTransaction {
                val specification = changeSet.new.copy(updatedAt = defaultUpdatedAt())
                dao.upsert(specification)
            }
        }
    }
}

internal class SafetyStockUpdateRepository(
    private val db: NocombroDatabase,
) : UpdateSingleLineRepository<SafetyStock> {

    private val dao = db.safetyStockDao

    override suspend fun getInit(id: Int): Result<SafetyStock> {
        return runCatching {
            dao.getByProductId(id) ?: SafetyStock(
                productId = id,
                reorderPoint = 0,
                orderQuantity = 0,
                id = 0
            )
        }
    }

    override suspend fun update(changeSet: ChangeSet<SafetyStock>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            db.inTransaction {
                MirrorDeletionJournalRepository(db).captureHardDeletesInCurrentTransaction {
                    with(changeSet.new) {
                        if (reorderPoint == 0L && orderQuantity == 0L) {
                            if (id > 0) {
                                dao.delete(this)
                            }
                        } else {
                            dao.upsert(copy(updatedAt = defaultUpdatedAt()))
                        }
                    }
                }
            }
        }
    }
}
internal class ProductDeclarationRepository(
    db: NocombroDatabase,
) : TransactionalUpdateCollectionRepository<ProductDeclarationIn, ProductDeclarationIn>(
    entitySyncKeyOf = ProductDeclarationIn::syncId,
    inWriteTransaction = { block -> db.inTransaction(block) },
    captureHardDeletes = MirrorDeletionJournalRepository(db)::captureHardDeletesInCurrentTransaction,
) {
    private val productDeclarationDao = db.productDeclarationDao
    private val declarationDao = db.declarationDao
    private val fileDao = db.fileDao

    override suspend fun getInit(id: Int): Result<List<ProductDeclarationIn>> {
        return runCatching {
            productDeclarationDao.getProductDeclarationIn(id)
        }
    }

    fun observeOnDeclarations(ids: List<Int>): Flow<List<Declaration>>{
        return declarationDao.observeDeclarationsByIds(ids)
    }

    suspend fun getDeclarationFiles(declarationId: Int): List<FileBD> {
        return fileDao.getFiles(declarationId, OwnerType.DECLARATION)
    }

    override fun prepareForUpsert(item: ProductDeclarationIn): ProductDeclarationIn {
        return item.copy(updatedAt = defaultUpdatedAt())
    }

    override suspend fun deleteByIds(ids: List<Int>) {
        productDeclarationDao.deleteProductDeclarations(ids)
    }

    override suspend fun upsertItems(items: List<ProductDeclarationIn>) {
        productDeclarationDao.upsertProductDeclarations(items)
    }
}
