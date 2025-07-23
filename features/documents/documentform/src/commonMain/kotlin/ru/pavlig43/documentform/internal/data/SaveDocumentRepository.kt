package ru.pavlig43.documentform.internal.data

import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.common.dbSafeCall
import ru.pavlig43.database.data.document.DocumentWithFiles
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.documentform.internal.ui.NAME_ALREADY_EXISTS_MESSAGE
import ru.pavlig43.documentform.internal.ui.NAME_IS_EMPTY_MESSAGE


internal class SaveDocumentRepository(
    private val documentDao: DocumentDao
) : ISaveDocumentRepository {
    override suspend fun saveItem(
        documentForSave: DocumentWithFiles,
        initLoadDocument: DocumentWithFiles
    ): RequestResult<Unit> {
        val name = documentForSave.displayName
        if (name.isBlank()) return RequestResult.Error(message = NAME_IS_EMPTY_MESSAGE)

        val nameIsExistResult: RequestResult<Unit> =
            if (documentForSave.displayName == initLoadDocument.displayName) {
                RequestResult.Success(Unit)
            } else {
                dbSafeCall(TAG) {
                    documentDao.isNameExist(documentForSave.displayName)
                }.let {
                    if (it.data == true) RequestResult.Error(message = NAME_ALREADY_EXISTS_MESSAGE) else it.mapTo { Unit }
                }
            }

        if (nameIsExistResult is RequestResult.Error) return nameIsExistResult

        return if (documentForSave.id == 0) {
            saveNewDocument(documentForSave)
        } else {
            updateDocument(documentForSave, initLoadDocument)
        }

    }

    private suspend fun saveNewDocument(documentForSave: DocumentWithFiles): RequestResult<Unit> {
        return dbSafeCall(TAG) {
            documentDao.insertDocumentWithWithFiles(documentForSave)
        }

    }

    private suspend fun updateDocument(
        documentForSave: DocumentWithFiles,
        initLoadDocument: DocumentWithFiles
    ): RequestResult<Unit> {
        return dbSafeCall(TAG) {
            documentDao.updateDocumentWithFiles(
                new = documentForSave,
                old = initLoadDocument
            )
        }
    }

    companion object {
        const val TAG = "SaveDocumentRepository"
    }
}
