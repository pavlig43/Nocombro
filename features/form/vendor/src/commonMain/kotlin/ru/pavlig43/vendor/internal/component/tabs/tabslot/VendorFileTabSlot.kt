package ru.pavlig43.vendor.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.pavlig43.addfile.api.component.FileComponent
import ru.pavlig43.addfile.api.FilesDependencies
import ru.pavlig43.database.data.files.OwnerType

internal class VendorFileTabSlot(
    componentContext: ComponentContext,
    vendorId: Int,
    dependencies: FilesDependencies,
) : FileComponent(
    componentContext = componentContext,
    ownerId = vendorId,
    ownerType = OwnerType.VENDOR,
    dependencies = dependencies
), VendorTabSlot {
    override val errorMessages: Flow<List<String>> = isAllFilesUpload.map { isUpload ->
        buildList {
            if (!isUpload) add("Идет загрузка")
        }
    }
}
