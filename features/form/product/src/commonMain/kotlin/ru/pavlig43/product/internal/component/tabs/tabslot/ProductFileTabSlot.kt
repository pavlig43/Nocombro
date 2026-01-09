package ru.pavlig43.product.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.addfile.api.component.FileComponent
import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.data.files.OwnerType

internal class ProductFileTabSlot(
    componentContext: ComponentContext,
    productId: Int,
    dependencies: FilesDependencies,
) : FileComponent(
    componentContext = componentContext,
    ownerId = productId,
    ownerType = OwnerType.PRODUCT,
    dependencies = dependencies
), ProductTabSlot {
    override val errorMessages: Flow<List<String>> = isAllFilesUpload.map { isUpload ->
        buildList {
            if (!isUpload) add("Идет загрузка")
        }
    }
}