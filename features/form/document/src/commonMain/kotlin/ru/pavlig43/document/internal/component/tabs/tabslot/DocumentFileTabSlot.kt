package ru.pavlig43.document.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
),DocumentTabSlot {
    override val errorMessages: Flow<List<String>> = flowOf(emptyList())
}

private fun FileUi.toFileData(documentId:Int): DocumentFile {
    return DocumentFile(
        documentId = documentId,
        path = path,
        id = id
    )
}

