package ru.pavlig43.productform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOutWithDocumentName
import ru.pavlig43.database.data.product.ProductFile
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository


internal val productFormModule = module {

    single<CreateItemRepository<Product>> { getCreateRepository(get()) }

    single<IUpdateRepository<Product>>(named(UpdateRepositoryType.Product.name)) {
        getInitItemRepository(
            get()
        )
    }

    single<UpdateCollectionRepository<ProductFile, ProductFile>>(
        named(
            UpdateCollectionRepositoryType.Files.name
        )
    ) { getFilesRepository(get()) }

    single<UpdateCollectionRepository<ProductDeclarationOutWithDocumentName, ProductDeclaration>>(
        named(UpdateCollectionRepositoryType.Declaration.name)
    ) { getDeclarationRepository(get()) }

}


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
    Declaration
}

private fun getInitItemRepository(
    db: NocombroDatabase
): IUpdateRepository<Product> {
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

private fun getDeclarationRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductDeclarationOutWithDocumentName, ProductDeclaration> {
    val declarationDao = db.productDeclarationDao
    return UpdateCollectionRepository(
        tag = "Product DeclarationRepository",
        loadCollection = declarationDao::getProductDeclarationWithDocumentName,
        deleteCollection = declarationDao::deleteDeclarations,
        upsertCollection = declarationDao::upsertProductDeclarations
    )
}
