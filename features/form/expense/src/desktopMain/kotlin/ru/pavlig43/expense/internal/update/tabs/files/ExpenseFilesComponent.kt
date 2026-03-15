package ru.pavlig43.expense.internal.update.tabs.files

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.component.FilesComponent

internal class ExpenseFilesComponent(
    componentContext: ComponentContext,
    expenseId: Int,
    dependencies: FilesDependencies,
) : FilesComponent(
    componentContext = componentContext,
    ownerId = expenseId,
    ownerType = OwnerType.EXPENSE,
    dependencies = dependencies
) {
    override val errorMessages: Flow<List<String>> = isAllFilesUpload.map { isUpload ->
        buildList {
            if (!isUpload) add("Идет загрузка")
        }
    }
}
