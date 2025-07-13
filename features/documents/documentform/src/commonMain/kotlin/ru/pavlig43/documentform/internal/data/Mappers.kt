package ru.pavlig43.documentform.internal.data

import io.github.vinceglb.filekit.absolutePath
import io.github.vinceglb.filekit.extension
import ru.pavlig43.addfile.api.data.AddedFile
import ru.pavlig43.database.data.document.DocumentFilePath

internal fun AddedFile.toDocumentFilePath(): DocumentFilePath {
    return DocumentFilePath(
        documentId = 0,
        filePath = platformFile.absolutePath(),
        fileExtension = platformFile.extension,
        id = 0
    )
}


