package ru.pavlig43.documentform.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.database.data.document.DocumentWithFiles

interface ISaveDocumentRepository {
    suspend fun saveItem(documentForSave: DocumentWithFiles,initLoadDocument:DocumentWithFiles): RequestResult<Unit>
}