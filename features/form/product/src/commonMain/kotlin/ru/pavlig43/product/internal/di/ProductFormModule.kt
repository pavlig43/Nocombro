package ru.pavlig43.product.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.core.model.UpsertListChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.multiLine.data.UpdateCollectionRepository
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository
import ru.pavlig43.product.api.ProductFormDependencies

internal fun createProductFormModule(dependencies: ProductFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateSingleItemRepository<Product>> { ProductCreateRepository(get()) }
        single<UpdateSingleLineRepository<Product>> { ProductUpdateRepository(get()) }


        single<UpdateCollectionRepository<ProductDeclarationOut, ProductDeclarationIn>>(
            UpdateCollectionRepositoryType.Declaration.qualifier
        ) { ProductDeclarationCollectionRepository(get()) }



        single<UpdateCollectionRepository<CompositionOut, CompositionIn>>(
            UpdateCollectionRepositoryType.Composition.qualifier
        ) { CompositionCollectionRepository(get()) }

    }

)


private class ProductCreateRepository(db: NocombroDatabase) : CreateSingleItemRepository<Product> {
    private val dao = db.productDao

    override suspend fun createEssential(item: Product): Result<Int> {
        return runCatching {
            dao.isCanSave(item).getOrThrow()
            dao.create(item).toInt()
        }
    }
}

private class ProductUpdateRepository(
    private val db: NocombroDatabase
) : UpdateSingleLineRepository<Product> {

    private val dao = db.productDao

    override suspend fun getInit(id: Int): Result<Product> {
        return runCatching {
            dao.getProduct(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<Product>): Result<Unit> {
        if (changeSet.old == changeSet.new) return Result.success(Unit)
        return runCatching {
            dao.isCanSave(changeSet.new).getOrThrow()
            dao.updateProduct(changeSet.new)
        }
    }
}


private class CompositionCollectionRepository(
    private val db: NocombroDatabase
) : UpdateCollectionRepository<CompositionOut, CompositionIn> {

    private val dao = db.compositionDao

    override suspend fun getInit(id: Int): Result<List<CompositionOut>> {
        return runCatching {
            dao.getCompositionOut(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<List<CompositionIn>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = { ids -> dao.deleteCompositions(ids) },
            upsert = { items -> dao.upsertComposition(items) }
        )
    }
}

internal enum class UpdateCollectionRepositoryType {

    Declaration,
    Composition,

}


private class ProductDeclarationCollectionRepository(
    private val db: NocombroDatabase
) : UpdateCollectionRepository<ProductDeclarationOut, ProductDeclarationIn> {

    private val dao = db.productDeclarationDao

    override suspend fun getInit(id: Int): Result<List<ProductDeclarationOut>> {
        return runCatching {
            dao.getProductDeclarationOut(id)
        }
    }

    override suspend fun update(changeSet: ChangeSet<List<ProductDeclarationIn>>): Result<Unit> {
        return UpsertListChangeSet.update(
            changeSet = changeSet,
            delete = { ids -> dao.deleteDeclarations(ids) },
            upsert = { items -> dao.upsertProductDeclarations(items) }
        )
    }
}



