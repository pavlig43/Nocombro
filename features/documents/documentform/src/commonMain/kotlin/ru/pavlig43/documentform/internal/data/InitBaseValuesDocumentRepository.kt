package ru.pavlig43.documentform.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.UTC
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.common.dbSafeCall
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository
import ru.pavlig43.manageitem.api.data.RequireValues


internal class InitBaseValuesDocumentRepository(
    private val documentDao: DocumentDao
): IInitDataRepository<RequireValues> {
    override suspend fun loadInitData(id: Int): RequestResult<RequireValues> {
        if (id == 0) return RequestResult.Success(getInitDataForState())
        return dbSafeCall(TAG){
            documentDao.getDocument(id)
        }.mapTo { document ->  document.toRequireValues()}
    }

    override fun getInitDataForState(): RequireValues {
        return RequireValues(
            id = 0,
            name = "",
            type = null,
            createdAt = null)

    }
    private companion object {
        const val TAG = "InitBaseValuesDocumentRepository"
    }
    private fun Document.toRequireValues(): RequireValues {
        return RequireValues(
            id = id,
            name = displayName,
            type = type,
            createdAt = createdAt
            )
    }
}

