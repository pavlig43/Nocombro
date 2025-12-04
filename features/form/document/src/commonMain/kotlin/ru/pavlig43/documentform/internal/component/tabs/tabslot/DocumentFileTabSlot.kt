package ru.pavlig43.documentform.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.addfile.api.component.UpdateFilesComponent
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.database.data.document.DocumentFile
import ru.pavlig43.update.data.UpdateCollectionRepository


internal class DocumentFileTabSlot(
    componentContext: ComponentContext,
    documentId: Int,
    updateRepository: UpdateCollectionRepository<DocumentFile,DocumentFile>
): UpdateFilesComponent<DocumentFile>(
    componentContext = componentContext,
    id = documentId,
    updateRepository = updateRepository,
    mapper = { toFileData(it)}
),DocumentTabSlot

private fun FileUi.toFileData(documentId:Int): DocumentFile {
    return DocumentFile(
        documentId = documentId,
        path = path,
        id = id
    )
}

