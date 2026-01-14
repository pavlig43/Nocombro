package ru.pavlig43.product.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.addfile.api.component.FilesComponent
import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.data.files.OwnerType

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