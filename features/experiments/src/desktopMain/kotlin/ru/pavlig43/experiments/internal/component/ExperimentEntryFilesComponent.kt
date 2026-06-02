package ru.pavlig43.experiments.internal.component

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.component.FilesComponent

internal class ExperimentEntryFilesComponent(
    componentContext: ComponentContext,
    entryId: Int,
    dependencies: FilesDependencies,
) : FilesComponent(
    componentContext = componentContext,
    ownerId = entryId,
    ownerType = OwnerType.EXPERIMENT_ENTRY,
    dependencies = dependencies,
) {
    override val errorMessages: Flow<List<String>> = isAllFilesUpload.map { isUpload ->
        buildList {
            if (!isUpload) add("Идет загрузка файлов")
        }
    }
}
