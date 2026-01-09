package ru.pavlig43.transaction.internal.di

import org.koin.dsl.module
import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.transaction.Transaction
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.transaction.api.TransactionFormDependencies
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createTransactionFormModule(dependencies: TransactionFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.dbTransaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<ImmutableTableDependencies> { dependencies.immutableTableDependencies }
        single<CreateEssentialsRepository<Transaction>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Transaction>> { getUpdateRepository(get()) }


    }

)


private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Transaction> {
    val dao = db.productTransactionDao
    return CreateEssentialsRepository(
        create = dao::create,
        isCanSave = dao::isCanSave
    )
}

private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Transaction> {
    val dao = db.productTransactionDao
    return UpdateEssentialsRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getTransaction,
        updateItem = dao::updateTransaction
    )
}

internal enum class UpdateCollectionRepositoryType {
    BatchesRow,
}




