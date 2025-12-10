package ru.pavlig43.transaction.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.product.ProductDeclaration
import ru.pavlig43.database.data.product.ProductDeclarationOut
import ru.pavlig43.database.data.transaction.ProductBatchTransactionBDIn
import ru.pavlig43.database.data.transaction.ProductBatchTransactionBDOut
import ru.pavlig43.database.data.transaction.ProductTransaction
import ru.pavlig43.itemlist.api.ItemListDependencies
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.update.data.UpdateCollectionRepository
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.dbTransaction }
        single<ItemListDependencies> { dependencies.itemListDependencies }
        single<CreateEssentialsRepository<ProductTransaction>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<ProductTransaction>> { getUpdateRepository(get()) }
        single<UpdateCollectionRepository<ProductBatchTransactionBDOut, ProductBatchTransactionBDIn>>(
            named(UpdateCollectionRepositoryType.BatchesRow.name)
        ) { getUpdateDeclarationRepository(get()) }


//        single<UpdateCollectionRepository<ProductFile, ProductFile>>(
//            named(
//                UpdateCollectionRepositoryType.Files.name
//            )
//        ) { getFilesRepository(get()) }
//

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
): UpdateEssentialsRepository<ProductTransaction> {
    val dao = db.productTransactionDao
    return UpdateEssentialsRepository(
        tag = "Update Transaction Repository",
        isCanSave = dao::isCanSave,
        loadItem = dao::getProductTransaction,
        updateItem = dao::updateTransaction
    )
}

internal enum class UpdateCollectionRepositoryType {
    BatchesRow,
}

private fun getUpdateDeclarationRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<ProductBatchTransactionBDOut, ProductBatchTransactionBDIn> {
    val dao = db.productBatchDao
    return UpdateCollectionRepository(
        tag = "Update Product Batch Respository",
        loadCollection = dao::getProductBatchesRow,
        deleteCollection = dao::deleteBatchesRows,
        upsertCollection = dao::upsert
    )
}


