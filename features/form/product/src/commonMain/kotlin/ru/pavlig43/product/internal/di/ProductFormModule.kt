package ru.pavlig43.product.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.create.data.CreateSingleItemRepository
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.CompositionIn
import ru.pavlig43.database.data.product.CompositionOut
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclarationIn
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.update.data.UpdateCollectionRepository
import ru.pavlig43.update.data.UpdateSingleLineRepository

internal fun createProductFormModule(dependencies: ProductFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateSingleItemRepository<Product>> { getCreateRepository(get()) }
        single<UpdateSingleLineRepository<Product>> { getUpdateRepository(get()) }


        single<UpdateCollectionRepository<ProductDeclarationOut, ProductDeclarationIn>>(
            UpdateCollectionRepositoryType.Declaration.qualifier
        ) { getUpdateDeclarationRepository(get()) }



        single<UpdateCollectionRepository<CompositionOut, CompositionIn>>(
            UpdateCollectionRepositoryType.Composition.qualifier
        ) { createUpdateCompositionRepository(get()) }

    }

)


private fun getCreateRepository(
    db: NocombroDatabase
): CreateSingleItemRepository<Product> {
    val dao = db.productDao
    return CreateSingleItemRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateSingleLineRepository<Product> {
    val dao = db.productDao
    return UpdateSingleLineRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getProduct,
        updateItem = dao::updateProduct
    )
}

internal enum class UpdateCollectionRepositoryType {

    Declaration,
    Composition,

}
private fun createUpdateCompositionRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<CompositionOut, CompositionIn>{
    val dao = db.compositionDao
    return UpdateCollectionRepository(
        loadCollection = dao::getCompositionOut,
        deleteCollection = dao::deleteCompositions,
        upsertCollection = dao::upsertComposition
    )
}


private fun getUpdateDeclarationRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductDeclarationOut, ProductDeclarationIn> {
    val dao = db.productDeclarationDao
    return UpdateCollectionRepository(
        loadCollection = dao::getProductDeclarationOut,
        deleteCollection = dao::deleteDeclarations,
        upsertCollection = dao::upsertProductDeclarations
    )
}


