package ru.pavlig43.documentform.internal.di

import org.koin.core.qualifier.named
import org.koin.dsl.module
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentFile
import ru.pavlig43.form.api.data.IUpdateRepository
import ru.pavlig43.form.api.data.UpdateItemRepository
import ru.pavlig43.manageitem.api.data.CreateItemRepository
import ru.pavlig43.upsertitem.api.data.UpdateCollectionRepository


internal val documentFormModule = module {

    single<CreateItemRepository<Document>> { getCreateRepository(get()) }
    single<IUpdateRepository<Document, Document>>(named(UpdateRepositoryType.Document.name)) { getInitItemRepository(get()) }
    single<UpdateCollectionRepository<DocumentFile,DocumentFile>>{ getFilesRepository(get()) }
}
private fun getCreateRepository(
    db: NocombroDatabase
): CreateItemRepository<Document> {
    val documentDao = db.documentDao
    return CreateItemRepository(
        tag = "Create Document Repository",
        isNameAllowed = documentDao::isNameAllowed,
        create = documentDao::create
    )
}

internal enum class UpdateRepositoryType{
    Document,
}
private fun getInitItemRepository(
    db: NocombroDatabase
): IUpdateRepository<Document, Document> {
    val documentDao = db.documentDao
    return UpdateItemRepository<Document>(
        tag = "Update Document Repository",
        loadItem = documentDao::getDocument,
        updateItem = documentDao::updateDocument
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



