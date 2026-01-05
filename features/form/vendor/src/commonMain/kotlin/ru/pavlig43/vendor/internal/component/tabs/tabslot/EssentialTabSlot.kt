package ru.pavlig43.vendor.internal.component.tabs.tabslot

import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.database.data.vendor.Vendor
import ru.pavlig43.update.component.UpdateEssentialsComponent
import ru.pavlig43.update.data.UpdateEssentialsRepository
import ru.pavlig43.vendor.internal.data.VendorEssentialsUi
import ru.pavlig43.vendor.internal.data.toDto

internal class EssentialTabSlot(
    componentContext: ComponentContext,
    vendorId: Int,
    updateRepository: UpdateEssentialsRepository<Vendor>,
    componentFactory: EssentialComponentFactory<Vendor, VendorEssentialsUi>,
) : UpdateEssentialsComponent<Vendor, VendorEssentialsUi>(
    componentContext = componentContext,
    id = vendorId,
    updateEssentialsRepository = updateRepository,
    componentFactory = componentFactory,
    mapperToDTO = {toDto()}
), VendorTabSlot {
    override val errorMessages: Flow<List<String>> = itemFields.map { vendor->
        buildList {
            if (vendor.displayName.isBlank()){
                add("Имя поставщика не должно быть пустым")
            }
        }

    }
}