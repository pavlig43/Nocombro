package ru.pavlig43.transaction.internal.di

import org.koin.core.qualifier.qualifier
import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.database.data.transaction.buy.BuyBD
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.update.data.UpdateCollectionRepository
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.dbTransaction }
        single<FilesDependencies> { dependencies.filesDependencies }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateEssentialsRepository<Transaction>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Transaction>> { getUpdateRepository(get()) }
        single<UpdateCollectionRepository<BuyBD, BuyBD>>(UpdateCollectionRepositoryType.BUY.qualifier) {
            createUpdateBuyRepository()
        }


    }

)


private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Transaction> {
    val dao = db.transactionDao
    return CreateEssentialsRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Transaction> {
    val dao = db.transactionDao
    return UpdateEssentialsRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getTransaction,
        updateItem = dao::updateTransaction
    )
}

internal enum class UpdateCollectionRepositoryType {

    BUY,

}

private fun createUpdateBuyRepository(
//    db: NocombroDatabase
): UpdateCollectionRepository<BuyBD, BuyBD> {
    return UpdateCollectionRepository(
        loadCollection = { emptyList() },
        deleteCollection = {},
        upsertCollection = {}
    )
}





