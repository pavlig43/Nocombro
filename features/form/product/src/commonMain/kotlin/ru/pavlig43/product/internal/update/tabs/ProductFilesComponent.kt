package ru.pavlig43.product.internal.update.tabs

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.database.data.files.OwnerType
import ru.pavlig43.files.api.FilesDependencies
import ru.pavlig43.files.api.component.FilesComponent

internal class ProductFilesComponent(
    componentContext: ComponentContext,
    productId: Int,
    dependencies: FilesDependencies,
) : FilesComponent(
    componentContext = componentContext,
    ownerId = productId,
    ownerType = OwnerType.PRODUCT,
    dependencies = dependencies
) {
    override val errorMessages: Flow<List<String>> = isAllFilesUpload.map { isUpload ->
        buildList {
            if (!isUpload) add("Идет загрузка")
        }
    }
}
