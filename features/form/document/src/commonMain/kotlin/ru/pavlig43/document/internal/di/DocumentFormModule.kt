package ru.pavlig43.document.internal.di

import org.koin.dsl.module
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.mutable.api.singleLine.data.CreateSingleItemRepository
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.api.DocumentFormDependencies
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.mutable.api.singleLine.data.UpdateSingleLineRepository

internal fun createDocumentFormModule(dependencies: DocumentFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateSingleItemRepository<Document>> { getCreateRepository(get()) }
        single<UpdateSingleLineRepository<Document>> { getUpdateRepository(get()) }
    }
)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateSingleItemRepository<Document> {
    val documentDao = db.documentDao
    return CreateSingleItemRepository(
        create = documentDao::create,
        isCanSave = documentDao::isCanSave
    )
}
private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateSingleLineRepository<Document>{
    val dao = db.documentDao
    return UpdateSingleLineRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getDocument,
        updateItem = dao::updateDocument
    )
}



