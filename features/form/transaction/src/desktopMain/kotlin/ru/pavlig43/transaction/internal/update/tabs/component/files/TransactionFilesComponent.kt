package ru.pavlig43.transaction.internal.update.tabs.component.files

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.component.FilesComponent

internal class TransactionFilesComponent(
    componentContext: ComponentContext,
    transactionId: Int,
    dependencies: FilesDependencies,
) : FilesComponent(
    componentContext = componentContext,
    ownerId = transactionId,
    ownerType = OwnerType.TRANSACTION,
    dependencies = dependencies,
) {
    override val errorMessages: Flow<List<String>> = isAllFilesUpload.map { isUpload ->
        buildList {
            if (!isUpload) add("Идет загрузка")
        }
    }
}
