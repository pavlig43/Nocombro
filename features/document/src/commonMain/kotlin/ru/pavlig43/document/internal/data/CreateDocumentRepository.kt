package ru.pavlig43.document.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.createitem.api.data.ICreateItemRepository
import ru.pavlig43.createitem.api.data.ValidNameResult
import ru.pavlig43.database.data.common.dbSafeGet
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.database.data.document.dao.DocumentDao

internal class CreateDocumentRepository(
    private val documentDao: DocumentDao
) : ICreateItemRepository<DocumentType> {
    override suspend fun isValidName(name: String): ValidNameResult {
        if (name.isBlank()) return ValidNameResult.Empty()
        return runCatching { documentDao.validName(name) }.fold(
            onSuccess = { if (it == 0)  ValidNameResult.Valid() else  ValidNameResult.AllReadyExists() },
            onFailure = {  ValidNameResult.Error(it.message ?: "Unknown error") }
        )

    }

    companion object {
        const val TAG = "CreateDocumentRepository"
    }
}
