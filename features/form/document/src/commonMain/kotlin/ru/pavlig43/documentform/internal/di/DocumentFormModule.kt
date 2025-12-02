package ru.pavlig43.documentform.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFile
import ru.pavlig43.documentform.api.DocumentFormDependencies
import ru.pavlig43.create.data.CreateEssentialsRepository
import ru.pavlig43.update.data.UpdateEssentialsRepository
import ru.pavlig43.update.data.UpdateCollectionRepository

internal fun createDocumentFormModule(dependencies: DocumentFormDependencies) = listOf(
    module {
        single<NocombroDatabase> { dependencies.db }
        single<DataBaseTransaction> { dependencies.transaction }
        single<CreateEssentialsRepository<Document>> { getCreateRepository(get()) }
        single<UpdateEssentialsRepository<Document>> { getUpdateRepository(get()) }
        single<UpdateCollectionRepository<DocumentFile,DocumentFile>>{ getFilesRepository(get()) }
    }
)

private fun getCreateRepository(
    db: NocombroDatabase
): CreateEssentialsRepository<Document> {
    val documentDao = db.documentDao
    return CreateEssentialsRepository(
        tag = "Create Document Repository",
        isNameAllowed = documentDao::isNameAllowed,
        create = documentDao::create
    )
}
private fun getUpdateRepository(
    db: NocombroDatabase
): UpdateEssentialsRepository<Document>{
    val dao = db.documentDao
    return UpdateEssentialsRepository(
        tag = "UpdateEssentialsRepository",
        isNameAllowed = dao::isNameAllowed,
        loadItem = dao::getDocument,
        updateItem = dao::updateDocument
    )
}

private fun getFilesRepository(
    db: NocombroDatabase
): UpdateCollectionRepository<DocumentFile,DocumentFile> {
    val fileDao = db.documentFilesDao
    return UpdateCollectionRepository(
        tag = "Document FilesRepository",
        loadCollection = fileDao::getFiles,
        deleteCollection = fileDao::deleteFiles,
        upsertCollection = fileDao::upsertDocumentFiles
    )
}



