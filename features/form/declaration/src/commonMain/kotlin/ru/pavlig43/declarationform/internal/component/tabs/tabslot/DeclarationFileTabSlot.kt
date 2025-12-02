package ru.pavlig43.declarationform.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.addfile.api.component.UpdateFilesComponent
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.database.data.declaration.DeclarationFile
import ru.pavlig43.update.data.UpdateCollectionRepository

internal class DeclarationFileTabSlot(
    componentContext: ComponentContext,
    declarationId: Int,
    updateRepository: UpdateCollectionRepository<DeclarationFile, DeclarationFile>
): UpdateFilesComponent<DeclarationFile>(
    componentContext = componentContext,
    id = declarationId,
    updateRepository = updateRepository,
    mapper = { toFileData(it)}
), DeclarationTabSlot

private fun FileUi.toFileData(declarationId:Int): DeclarationFile {
    return DeclarationFile(
        declarationId = declarationId,
        path = path,
        id = id
    )
}