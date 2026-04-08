package ru.pavlig43.product.internal.di

import kotlinx.coroutines.flow.Flow
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.UpsertListChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.product.COMPOSITION_TABLE_NAME
import ru.pavlig43.database.data.product.PRODUCT_TABLE_NAME
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.SafetyStock
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.SyncUpdateCollectionRepository
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncUpdateSingleLineRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.product.api.ProductFormDependencies

internal fun createProductFormModule(dependencies: ProductFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateSingleItemRepository<Product>> { ProductCreateRepository(get(), get()) }
        single<UpdateSingleLineRepository<Product>> (SingleRepositoryType.ESSENTIALS.qualifier){ ProductUpdateRepository(get(), get()) }

        single<UpdateCollectionRepository<CompositionOut, CompositionIn>>(
            UpdateCollectionRepositoryType.Composition.qualifier
        ) { CompositionCollectionRepository(get(), get()) }

        singleOf(::ProductDeclarationRepository)

        single<UpdateSingleLineRepository<SafetyStock>>(SingleRepositoryType.SAFETY.qualifier) { SafetyStockUpdateRepository(get()) }
    }
)

private class ProductCreateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncCreateSingleItemRepository<Product>(
    tableName = PRODUCT_TABLE_NAME,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {
    private val dao = db.productDao

    override suspend fun validate(item: Product): Result<Unit> = dao.isCanSave(item)

    override suspend fun createInDb(item: Product): Int = dao.create(item).toInt()
}

private class ProductUpdateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateSingleLineRepository<Product>(
    tableName = PRODUCT_TABLE_NAME,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
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
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateCollectionRepository<CompositionOut, CompositionIn>(
    tableName = COMPOSITION_TABLE_NAME,
    entitySyncKeyOf = CompositionIn::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    enqueueSyncDelete = syncQueueRepository::enqueueDelete,
    inWriteTransaction = { block -> db.inTransaction(block) },
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
    SAFETY
}

internal class SafetyStockUpdateRepository(
    db: NocombroDatabase
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
            with(changeSet.new){
                if (reorderPoint == 0L && orderQuantity == 0L){
                    dao.delete(this)
                }
                else{
                    dao.upsert(this)
                }
            }
        }
    }
}
internal class ProductDeclarationRepository(
    db: NocombroDatabase
) {
    private val productDeclarationDao = db.productDeclarationDao
    private val declarationDao = db.declarationDao

    suspend fun getInit(productId: Int): Result<List<ProductDeclarationIn>> {
        return runCatching {
            productDeclarationDao.getProductDeclarationIn(productId)
        }
    }
    fun observeOnDeclarations(ids: List<Int>): Flow<List<Declaration>>{
        return declarationDao.observeDeclarationsByIds(ids)
    }
    suspend fun update(changeSet: ChangeSet<List<ProductDeclarationIn>>): Result<Unit>{
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = productDeclarationDao::deleteProductDeclarations,
            upsert = productDeclarationDao::upsertProductDeclarations
        )
    }
}
