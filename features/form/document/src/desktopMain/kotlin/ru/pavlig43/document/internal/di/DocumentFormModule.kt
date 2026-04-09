package ru.pavlig43.document.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.model.ChangeSet
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DOCUMENT_TABLE_NAME
import ru.pavlig43.database.data.sync.SyncQueueRepository
import ru.pavlig43.database.data.sync.defaultUpdatedAt
import ru.pavlig43.database.inTransaction
import ru.pavlig43.document.api.DocumentFormDependencies
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncCreateSingleItemRepository
import ru.pavlig43.mutable.api.singleLine.data.SyncUpdateSingleLineRepository
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository

internal fun createDocumentFormModule(dependencies: DocumentFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single { SyncQueueRepository(get<NocombroDatabase>().syncDao) }
        single<CreateSingleItemRepository<Document>> { DocumentCreateRepository(get(), get()) }
        single<UpdateSingleLineRepository<Document>> { DocumentUpdateRepository(get(), get()) }
    }
)

private class DocumentCreateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncCreateSingleItemRepository<Document>(
    tableName = DOCUMENT_TABLE_NAME,
    entitySyncKeyOf = Document::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {
    private val dao = db.documentDao

    override suspend fun validate(item: Document): Result<Unit> = dao.isCanSave(item)

    override suspend fun createInDb(item: Document): Int = dao.create(item).toInt()
}

private class DocumentUpdateRepository(
    db: NocombroDatabase,
    syncQueueRepository: SyncQueueRepository,
) : SyncUpdateSingleLineRepository<Document>(
    tableName = DOCUMENT_TABLE_NAME,
    entitySyncKeyOf = Document::syncId,
    enqueueSyncUpsert = syncQueueRepository::enqueueUpsert,
    inWriteTransaction = { block -> db.inTransaction(block) },
) {

    private val dao = db.documentDao

    override suspend fun getInit(id: Int): Result<Document> {
        return runCatching {
            dao.getDocument(id)
        }
    }

    override fun prepareForUpdate(item: Document): Document = item.copy(updatedAt = defaultUpdatedAt())

    override suspend fun validate(item: Document): Result<Unit> = dao.isCanSave(item)

    override suspend fun updateInDb(item: Document) {
        dao.updateDocument(item)
    }
}




