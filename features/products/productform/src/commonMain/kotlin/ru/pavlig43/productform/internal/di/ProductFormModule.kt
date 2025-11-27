package ru.pavlig43.productform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductCompositionIn
import ru.pavlig43.database.data.product.ProductCompositionOut
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOutWithNameAndVendor
import ru.pavlig43.database.data.product.ProductFile
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.productform.api.ProductFormDependencies
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository

internal fun createProductFormModule(dependencies: ProductFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<CreateItemRepository<Product>> { getCreateRepository(get()) }

        single<IUpdateRepository<Product, Product>>(named(UpdateRepositoryType.Product.name)) {
            getInitItemRepository(
                get()
            )
        }

        single<UpdateCollectionRepository<ProductFile, ProductFile>>(
            named(
                UpdateCollectionRepositoryType.Files.name
            )
        ) { getFilesRepository(get()) }

        single<UpdateCollectionRepository<ProductDeclarationOutWithNameAndVendor, ProductDeclaration>>(
            named(UpdateCollectionRepositoryType.Declaration.name)
        ) { getUpdateDeclarationRepository(get()) }

        single<UpdateCollectionRepository<ProductCompositionOut, ProductCompositionIn>>(
            named(UpdateCollectionRepositoryType.Composition.name)
        ) { getUpdateCompositionRepository(get()) }

    }

)


private fun getCreateRepository(
    db: NocombroDatabase
): CreateItemRepository<Product> {
    val productDao = db.productDao
    return CreateItemRepository(
        tag = "Create Product Repository",
        isNameAllowed = productDao::isNameAllowed,
        create = productDao::create
    )
}

internal enum class UpdateRepositoryType {
    Product,
}

internal enum class UpdateCollectionRepositoryType {
    Files,
    Declaration,
    Composition,

}

private fun getInitItemRepository(
    db: NocombroDatabase
): IUpdateRepository<Product, Product> {
    val productDao = db.productDao
    return UpdateItemRepository<Product>(
        tag = "Update Product Repository",
        loadItem = productDao::getProduct,
        updateItem = productDao::updateProduct
    )
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
):UpdateCollectionRepository<ProductDeclarationOutWithNameAndVendor, ProductDeclaration>{
    val dao =db.productDeclarationDao
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


