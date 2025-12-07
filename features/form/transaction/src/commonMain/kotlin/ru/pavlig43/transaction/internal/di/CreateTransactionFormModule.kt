package ru.pavlig43.transaction.internal.di

import org.koin.dsl.module
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.Product
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<ItemListDependencies> { dependencies.itemListDependencies }
        single<CreateEssentialsRepository<ProductTransaction>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Product>> { getUpdateRepository(get()) }



//        single<UpdateCollectionRepository<ProductFile, ProductFile>>(
//            named(
//                UpdateCollectionRepositoryType.Files.name
//            )
//        ) { getFilesRepository(get()) }
//
//        single<UpdateCollectionRepository<ProductDeclarationOut, ProductDeclaration>>(
//            named(UpdateCollectionRepositoryType.Declaration.name)
//        ) { getUpdateDeclarationRepository(get()) }
//
//        single<UpdateCollectionRepository<ProductCompositionOut, ProductCompositionIn>>(
//            named(UpdateCollectionRepositoryType.Composition.name)
//        ) { getUpdateCompositionRepository(get()) }

    }

)


private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<ProductTransaction> {
    val dao = db.productTransactionDao
    return CreateEssentialsRepository(
        tag = "Create Transaction Repository",
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Product> {
    val dao = db.productDao
    return UpdateEssentialsRepository(
        tag = "Update Product sRepository",
        isCanSave = dao::isCanSave,
        loadItem = dao::getProduct,
        updateItem = dao::updateProduct
    )
}

//internal enum class UpdateCollectionRepositoryType {
//    Files,
//    Declaration,
//    Composition,
//
//}


