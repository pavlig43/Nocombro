package ru.pavlig43.documentlist.internal.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.database.data.common.dbSafeCall
import ru.pavlig43.database.data.common.dbSafeFlow
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.documentlist.api.data.DocumentUi
import ru.pavlig43.itemlist.api.data.IItemListRepository

/**
 * TODO ПЕРЕДЕЛАТЬ
 */
internal class DocumentListRepository(
    private val documentDao: DocumentDao
) : IItemListRepository<Document, DocumentUi,DocumentType>{

    override suspend fun deleteItemsById(ids: List<Int>): RequestResult<Unit> {
        return dbSafeCall(TAG){
            documentDao.deleteDocuments(ids)
        }
    }

    override fun getAllItem(): Flow<RequestResult<List<Document>>> {
        return dbSafeFlow(TAG){
            documentDao.getAllDocuments()
        }
    }

    override fun getAllItemTypes(): List<DocumentType> {
        return DocumentType.entries
    }

    override fun getItemsByTypes(types: List<DocumentType>): Flow<RequestResult<List<Document>>> {
        return dbSafeFlow(TAG){
            documentDao.getDocumentsByTypes(types,)

        }
    }
    override fun toItemUi(item: Document): DocumentUi {
    return DocumentUi(
        id = item.id,
        displayName = item.displayName,
        type = item.type,
        createdAt = item.createdAt.convertToDateTime()
    )
}
    companion object{
        const val TAG = "DocumentRepository"
    }

}

