package ru.pavlig43.document.internal.data

import kotlinx.coroutines.flow.Flow
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.convertToDateTime
import ru.pavlig43.database.data.common.dbSafeDelete
import ru.pavlig43.database.data.common.dbSafeFlow
import ru.pavlig43.database.data.common.dbSafeGet
import ru.pavlig43.database.data.common.dbSafeInsert
import ru.pavlig43.database.data.common.dbSafeUpdate
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.document.api.data.DocumentUi
import ru.pavlig43.itemlist.api.data.IItemRepository

/**
 * TODO ПЕРЕДЕЛАТЬ
 */
class DocumentRepository(
    private val documentDao: DocumentDao
) : IItemRepository<Document, DocumentUi,DocumentType>{
    override suspend fun insert(item: Document): RequestResult<Unit> {
        return dbSafeInsert(TAG){
            documentDao.insert(item)
        }
    }

    override suspend fun update(item: Document): RequestResult<Int> {
        return dbSafeUpdate(TAG){
            documentDao.update(item)
        }
    }

    override suspend fun deleteItemsById(ids: List<Int>): RequestResult<Int> {
        return dbSafeDelete(TAG){
            documentDao.deleteItems(ids)
        }
    }

    override suspend fun getItem(id: Int): RequestResult<Document> {
        return dbSafeGet(TAG){
            documentDao.getDocument(id)
        }
    }

    override fun getAllItem(): Flow<RequestResult<List<Document>>> {
        return dbSafeFlow(TAG){
            documentDao.getAllDocument()
        }
    }

    override fun getAllItemTypes(): List<DocumentType> {
        return DocumentType.entries
    }

    override fun toItemUi(item: Document): DocumentUi {
        return DocumentUi(
            id = item.id,
            displayName = item.displayName,
            type = item.type,
            createdAt = item.createdAt.convertToDateTime()
        )
    }

    override fun getItemsByTypes(types: List<DocumentType>): Flow<RequestResult<List<Document>>> {

        return dbSafeFlow(TAG){
            documentDao.getDocumentsByTypes(types,)

        }
    }
    companion object{
        const val TAG = "DocumentRepository"
    }

}
