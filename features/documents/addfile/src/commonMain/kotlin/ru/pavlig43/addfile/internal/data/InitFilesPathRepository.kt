package ru.pavlig43.addfile.internal.data

import io.github.vinceglb.filekit.PlatformFile
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.addfile.api.data.UploadState
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.mapTo
import ru.pavlig43.database.data.common.dbSafeCall
import ru.pavlig43.database.data.document.DocumentFilePath
import ru.pavlig43.database.data.document.dao.DocumentDao
import ru.pavlig43.loadinitdata.api.data.IInitDataRepository


internal class InitFilesPathRepository(
    private val documentDao: DocumentDao
) : IInitDataRepository<List<AddedFile>> {
    override suspend fun loadInitData(id: Int): RequestResult<List<AddedFile>> {
        if (id == 0) return RequestResult.Success(getInitDataForState())
        return dbSafeCall(TAG) {
            documentDao.getFilePaths(id)
        }.mapTo { lst ->
            lst.mapIndexed { index, documentFilePath ->
                documentFilePath.toAddedFile(
                    index
                )
            }
        }
    }

    override fun getInitDataForState(): List<AddedFile> = emptyList()

    private fun DocumentFilePath.toAddedFile(composeKey: Int): AddedFile {
        return AddedFile(
            id = id,
            composeKey = composeKey,
            platformFile = PlatformFile(filePath),
            uploadState = UploadState.Success
        )
    }

    private companion object {
        const val TAG = "InitFilesPathRepository"
    }
}