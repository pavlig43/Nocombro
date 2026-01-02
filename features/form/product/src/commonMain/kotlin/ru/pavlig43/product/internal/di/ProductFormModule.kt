package ru.pavlig43.product.internal.di

import org.koin.core.qualifier.named
import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.*
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.update.data.UpdateCollectionRepository
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createProductFormModule(dependencies: ProductFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<ImmutableTableDependencies> { dependencies.dependencies }
        single<CreateEssentialsRepository<Product>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Product>> { getUpdateRepository(get()) }



        single<UpdateCollectionRepository<ProductFile, ProductFile>>(

                UpdateCollectionRepositoryType.Files.qualifier

        ) { getFilesRepository(get()) }

        single<UpdateCollectionRepository<ProductDeclarationOut, ProductDeclarationIn>>(
            UpdateCollectionRepositoryType.Declaration.qualifier
        ) { getUpdateDeclarationRepository(get()) }

        single<UpdateCollectionRepository<ProductCompositionOut, ProductCompositionIn>>(
            UpdateCollectionRepositoryType.Composition.qualifier
        ) { getUpdateCompositionRepository(get()) }

        single<UpdateCollectionRepository<CompositionOut, CompositionIn>>(
            UpdateCollectionRepositoryType.Composition1.qualifier
        ) { createUpdateCompositionRepository(get()) }

    }

)


private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Product> {
    val dao = db.productDao
    return CreateEssentialsRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Product> {
    val dao = db.productDao
    return UpdateEssentialsRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getProduct,
        updateItem = dao::updateProduct
    )
}

internal enum class UpdateCollectionRepositoryType {
    Files,
    Declaration,
    Composition,
    Composition1,

}
private fun createUpdateCompositionRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<CompositionOut, CompositionIn>{
    val dao = db.compositionDao1
    return UpdateCollectionRepository(
        loadCollection = dao::getCompositionOut,
        deleteCollection = dao::deleteCompositions,
        upsertCollection = dao::upsertComposition
    )
}
private fun getFilesRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductFile, ProductFile> {
    val fileDao = db.productFilesDao
    return UpdateCollectionRepository(
        loadCollection = fileDao::getFiles,
        deleteCollection = fileDao::deleteFiles,
        upsertCollection = fileDao::upsertProductFiles
    )
}

private fun getUpdateDeclarationRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductDeclarationOut, ProductDeclarationIn> {
    val dao = db.productDeclarationDao
    return UpdateCollectionRepository(
        loadCollection = dao::getProductDeclarationWithDocumentName,
        deleteCollection = dao::deleteDeclarations,
        upsertCollection = dao::upsertProductDeclarations
    )
}


private fun getUpdateCompositionRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductCompositionOut, ProductCompositionIn> {
    val dao = db.compositionDao
    return UpdateCollectionRepository(
        loadCollection = dao::getCompositions,
        deleteCollection = dao::deleteCompositions,
        upsertCollection = dao::upsertCompositions
    )
}


