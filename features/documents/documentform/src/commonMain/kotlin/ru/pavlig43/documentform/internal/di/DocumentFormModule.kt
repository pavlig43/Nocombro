package ru.pavlig43.documentform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFile
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.database.data.document.dao.DocumentFilesDao
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository


internal val documentFormModule = module {
    single<CreateItemRepository<Document>> { getCreateRepository(get()) }
    single<IUpdateRepository<Document>>(named(UpdateRepositoryType.Document.name)) { getInitItemRepository(get()) }
    single<UpdateCollectionRepository<DocumentFile,DocumentFile>>{ getFilesRepository(get()) }
}
private fun getCreateRepository(
    documentDao: DocumentDao
): CreateItemRepository<Document> {
    return CreateItemRepository(
        tag = "Create Document Repository",
        create = documentDao::create,
        isNameExist = documentDao::isNameExist
    )
}

internal enum class UpdateRepositoryType{
    Document,
}
private fun getInitItemRepository(
    documentDao: DocumentDao
): IUpdateRepository<Document> {
    return UpdateItemRepository<Document>(
        tag = "Update Document Repository",
        loadItem = documentDao::getDocument,
        updateItem = documentDao::updateDocument
    )
}

private fun getFilesRepository(
    fileDao: DocumentFilesDao
): UpdateCollectionRepository<DocumentFile,DocumentFile> {
    return UpdateCollectionRepository(
        tag = "Document FilesRepository",
        loadCollection = fileDao::getFiles,
        deleteCollection = fileDao::deleteFiles,
        upsertCollection = fileDao::upsertDocumentFiles
    )
}



