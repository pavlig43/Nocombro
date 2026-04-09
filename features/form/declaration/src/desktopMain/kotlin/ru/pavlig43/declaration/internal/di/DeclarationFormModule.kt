package ru.pavlig43.declaration.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.database.data.declaration.DECLARATIONS_TABLE_NAME
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction
import ru.pavlig43.declaration.api.DeclarationFormDependencies
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.immutable.api.ImmutableTableDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncUpdateSingleLineRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository

internal fun createDeclarationFormModule(dependencies: DeclarationFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<ImmutableTableDependencies> {dependencies.immutableTableDependencies  }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single { SyncQueueRepository(get<NocombroDatabase>().syncDao) }
        single<CreateSingleItemRepository<Declaration>> { CreateDeclarationRepository(get(), get()) }
        single<UpdateSingleLineRepository<Declaration>> {  DeclarationUpdateRepository(get(), get())}

    }

)

private class CreateDeclarationRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncCreateSingleItemRepository<Declaration>(
    tableName = DECLARATIONS_TABLE_NAME,
    entitySyncKeyOf = Declaration::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {
    private val dao = db.declarationDao

    override suspend fun validate(item: Declaration): Result<Unit> = dao.isCanSave(item)

    override suspend fun createInDb(item: Declaration): Int = dao.create(item).toInt()
}
private class DeclarationUpdateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateSingleLineRepository<Declaration>(
    tableName = DECLARATIONS_TABLE_NAME,
    entitySyncKeyOf = Declaration::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.declarationDao

    override suspend fun getInit(id: Int): Result<Declaration> {
        return runCatching {
            dao.getDeclaration(id)
        }
    }

    override fun prepareForUpdate(item: Declaration): Declaration = item.copy(updatedAt = defaultUpdatedAt())

    override suspend fun validate(item: Declaration): Result<Unit> = dao.isCanSave(item)

    override suspend fun updateInDb(item: Declaration) {
        dao.updateDeclaration(item)
    }
}


