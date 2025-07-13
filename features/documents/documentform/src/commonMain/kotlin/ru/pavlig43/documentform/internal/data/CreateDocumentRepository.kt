package ru.pavlig43.documentform.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.createitem.api.data.IItemFormRepository
import ru.pavlig43.createitem.api.data.ValidNameResult
import ru.pavlig43.database.data.common.dbSafeCall
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.database.data.document.dao.DocumentDao

internal class CreateDocumentRepository(
    private val documentDao: DocumentDao
) : IItemFormRepository<DocumentWithFiles,DocumentType> {
    override suspend fun isValidName(name: String): ValidNameResult {
        if (name.isBlank()) return ValidNameResult.Empty()

        return runCatching { documentDao.isExistName(name) }.fold(
            onSuccess = { if (it == 0)  ValidNameResult.Valid() else  ValidNameResult.AllReadyExists() },
            onFailure = {  ValidNameResult.Error(it.message ?: "Unknown error") }
        )

    }

    override suspend fun saveItem(item:DocumentWithFiles): RequestResult<Unit> {
        return dbSafeCall(TAG){
            documentDao.insertDocumentWithWithFiles(item)
        }

    }

    companion object {
        const val TAG = "CreateDocumentRepository"
    }
}
