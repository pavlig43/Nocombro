package ru.pavlig43.product.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.*
import ru.pavlig43.itemlist.api.dependencies
import ru.pavlig43.product.api.ProductFormDependencies
import ru.pavlig43.update.data.UpdateCollectionRepository
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createProductFormModule(dependencies: ProductFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<dependencies> { dependencies.dependencies }
        single<CreateEssentialsRepository<Product>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Product>> { getUpdateRepository(get()) }



        single<UpdateCollectionRepository<ProductFile, ProductFile>>(
            named(
                UpdateCollectionRepositoryType.Files.name
            )
        ) { getFilesRepository(get()) }

        single<UpdateCollectionRepository<ProductDeclarationOut, ProductDeclaration>>(
            named(UpdateCollectionRepositoryType.Declaration.name)
        ) { getUpdateDeclarationRepository(get()) }

        single<UpdateCollectionRepository<ProductCompositionOut, ProductCompositionIn>>(
            named(UpdateCollectionRepositoryType.Composition.name)
        ) { getUpdateCompositionRepository(get()) }

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

}

private fun getFilesRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductFile, ProductFile> {
    val fileDao = db.productFilesDao
    return UpdateCollectionRepository(
        tag = "Product FilesRepository",
        loadCollection = fileDao::getFiles,
        deleteCollection = fileDao::deleteFiles,
        upsertCollection = fileDao::upsertProductFiles
    )
}

private fun getUpdateDeclarationRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductDeclarationOut, ProductDeclaration> {
    val dao = db.productDeclarationDao
    return UpdateCollectionRepository(
        tag = "Update Collection Respository",
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
        tag = "Product Composition Repository",
        loadCollection = dao::getCompositions,
        deleteCollection = dao::deleteCompositions,
        upsertCollection = dao::upsertCompositions
    )
}


