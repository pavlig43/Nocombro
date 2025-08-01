package ru.pavlig43.documentlist.internal.di

import org.koin.dsl.module
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.itemlist.api.data.ItemListRepository

internal val documentListModule = module {
    single<ItemListRepository<Document,DocumentType>> { getDocumentListRepository(get()) }

}
private fun getDocumentListRepository(
    documentDao: DocumentDao,
): ItemListRepository<Document, DocumentType> {
    return ItemListRepository<Document,DocumentType>(
        tag = "DocumentRepository",
        deleteByIds = documentDao::deleteDocuments,
        observeAllItem = documentDao::observeAllDocuments,
        observeItemsByTypes = documentDao::observeDocumentsByTypes
    )
}