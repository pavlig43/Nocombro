package ru.pavlig43.document.internal.di

import org.koin.dsl.module
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.document.api.DocumentFormDependencies
import ru.pavlig43.update.data.UpdateEssentialsRepository

internal fun createDocumentFormModule(dependencies: DocumentFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<TransactionExecutor> { dependencies.transaction }
        single<FilesDependencies> {dependencies.filesDependencies  }
        single<CreateEssentialsRepository<Document>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Document>> { getUpdateRepository(get()) }
    }
)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Document> {
    val documentDao = db.documentDao
    return CreateEssentialsRepository(
        create = documentDao::create,
        isCanSave = documentDao::isCanSave
    )
}
private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Document>{
    val dao = db.documentDao
    return UpdateEssentialsRepository(
        isCanSave = dao::isCanSave,
        loadItem = dao::getDocument,
        updateItem = dao::updateDocument
    )
}



