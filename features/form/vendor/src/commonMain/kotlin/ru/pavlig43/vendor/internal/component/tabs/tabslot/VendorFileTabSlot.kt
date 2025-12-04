package ru.pavlig43.vendor.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import ru.pavlig43.addfile.api.component.UpdateFilesComponent
import ru.pavlig43.addfile.api.data.FileUi
import ru.pavlig43.database.data.vendor.VendorFile
import ru.pavlig43.update.data.UpdateCollectionRepository

internal class VendorFileTabSlot(
    componentContext: ComponentContext,
    vendorId: Int,
    updateRepository: UpdateCollectionRepository<VendorFile, VendorFile>
): UpdateFilesComponent<VendorFile>(
    componentContext = componentContext,
    id = vendorId,
    updateRepository = updateRepository,
    mapper = { toFileData(it)}
), VendorTabSlot

private fun FileUi.toFileData(vendorId:Int): VendorFile {
    return VendorFile(
        vendorId = vendorId,
        path = path,
        id = id
    )
}