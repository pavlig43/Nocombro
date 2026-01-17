package ru.pavlig43.document.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import ru.pavlig43.files.api.component.FilesComponent
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.database.data.files.OwnerType


internal class DocumentFilesComponent(
    componentContext: ComponentContext,
    documentId: Int,
    dependencies: FilesDependencies,
): FilesComponent(
    componentContext = componentContext,
    ownerId = documentId,
    ownerType = OwnerType.DOCUMENT,
    dependencies = dependencies
) {
    override val errorMessages: Flow<List<String>> = combine(
        isAllFilesUpload,
        filesUi
    ){isUpload,files->
        buildList { 
            if (!isUpload) add( "Идет загрузка")
            if (files.isEmpty()) add("Добавь хотя бы один файл")
        }
    }
}

